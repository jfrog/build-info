# GoZipBallStreamer Decision Tree

This document details every decision path in `GoZipBallStreamer.java` -- the component responsible for transforming a raw source zipball (downloaded from GitHub/GitLab/Bitbucket) into a Go module zip conforming to the Go module zip specification.

---

## High-Level Pipeline

```
Input: Source zipball (e.g., GitHub tarball)
  |
  v
1. initiateProjectType()   --> Determines subModuleName
2. scanEntries()           --> Finds submodules, LICENSE, go.mod version; builds excludedDirectories
3. writeEntries()          --> Writes each non-excluded entry with corrected paths
  |
  v
Output: Deployable Go module zip (projectName@version/...)
```

---

## Phase 1: Project Type Detection (`initiateProjectType`)

```
                    +---------------------------+
                    | subModuleNameExplicitlySet |
                    |    (set by fetcher)?       |
                    +---------------------------+
                           |           |
                          YES          NO
                           |           |
                           v           v
               applyExplicitSubModule()   isCompatibleModuleFromV2()?
                                          getMajorVersion(version) >= 2
                                          AND isCompatibleGoModuleNaming(projectName, version)
                                               |              |
                                              YES             NO
                                               |              |
                                               v              v
                             detectSubModuleForCompatibleV2()  detectSubModuleForStandardModule()
```

---

### Branch A: Explicit SubModule (`subModuleNameExplicitlySet = true`)

This branch is taken when a fetcher (e.g., GitLabIntelligentFetcher) has already determined the submodule name.

```
applyExplicitSubModule()
  |
  v
correctExplicitMajorVersionSubModule()
  |
  +-- Is subModuleName empty? --> YES: skip, keep empty
  |
  +-- NO: Is majorVersion >= 2 AND subModuleName == "vN" (e.g., "v2")
  |        AND hasRootModFileOfCompatibleModuleFromV2("vN")?
  |           |
  |          YES --> subModuleName = "" (pack from root, compatible layout)
  |          NO  --> keep subModuleName as-is
  |
  v
Result: subModuleName is finalized
```

**Examples:**

| projectName | version | Explicit subModuleName | Root go.mod | Final subModuleName | Behavior |
|---|---|---|---|---|---|
| `gitlab.com/group/project` | `v1.0.0` | `""` (empty) | N/A | `""` | Pack from root |
| `gitlab.com/group/project` | `v1.0.0` | `"api"` | N/A | `"api"` | Pack submodule `api/` |
| `gitlab.com/group/project/v2` | `v2.0.0` | `"v2"` | `module .../v2` | `""` | Override: compatible v2 at root |
| `gitlab.com/group/project/v2` | `v2.0.0` | `"v2"` | `module .../project` (no /v2) | `"v2"` | Physical v2/ directory |

---

### Branch B: Compatible Module from v2+ (`isCompatibleModuleFromV2()` = true)

Triggered when:
- `getMajorVersion(version) >= 2` (version is v2.0.0 or higher)
- `isCompatibleGoModuleNaming(projectName, version)` is true (projectName ends with `/vN` matching the major version)

```
detectSubModuleForCompatibleV2()
  |
  v
majorVersion = "v" + getMajorProjectVersion(projectName)   e.g. "v2"
subModule = getSubModule(projectName)                       e.g. "submodule/v2" or "v2" or ""
  |
  v
isNestedSubModule = (subModule is not empty) AND (subModule != majorVersion)
  |
  v
+-- hasRootModFileOfCompatibleModuleFromV2(majorVersion) AND NOT isNestedSubModule?
|      |
|     YES --> subModuleName stays "" (pack from root, root go.mod declares /vN)
|     NO  --> continue below
|
v
subModuleName = subModule
  |
  v
+-- isSubModuleWithMajorVersion(subModuleName)?   (matches pattern .*?/v\d+)
|   AND NOT hasModFileAtSubModulePath(subModuleName)?
|      |
|     YES --> Strip /vN: subModuleName = subModuleName without last "/vN"
|             (branch/tag layout, no physical vN directory)
|     NO  --> Keep subModuleName as-is (subdirectory layout, physical vN/ exists)
|
v
Result: subModuleName is finalized
```

**Examples:**

| projectName | version | getSubModule | Root go.mod | Zip has subModule/vN/go.mod? | isNestedSubModule | Final subModuleName | Description |
|---|---|---|---|---|---|---|---|
| `github.com/owner/repo/v2` | `v2.0.0` | `"v2"` | `module .../v2` | N/A | `false` (v2 == majorVersion) | `""` | Compatible v2 at root |
| `github.com/owner/repo/v3` | `v3.1.0` | `"v3"` | `module .../v2` (wrong!) | N/A | `false` | `"v3"` | Root go.mod doesn't match v3 |
| `github.com/owner/repo/submodule/v2` | `v2.0.1` | `"submodule/v2"` | `module .../repo` | YES (has `submodule/v2/go.mod`) | `true` | `"submodule/v2"` | Subdirectory layout (physical dir) |
| `github.com/owner/repo/foo/v2` | `v2.1.0` | `"foo/v2"` | `module .../repo` | NO (no `foo/v2/go.mod`) | `true` | `"foo"` | Branch/tag layout (strip /v2) |
| `github.com/thepudds/nested/contrib/nested1/v2` | `v2.0.0` | `"contrib/nested1/v2"` | `module .../v2` | NO | `true` | `"contrib/nested1"` | Nested + root v2, branch layout |
| `github.com/thepudds/nested/v2` | `v2.0.0` | `"v2"` | `module .../v2` | N/A | `false` | `""` | Root v2 module (no nested) |

---

### Branch C: Standard Module (not compatible v2+)

Triggered when:
- Major version is 0 or 1, OR
- `isCompatibleGoModuleNaming` is false (e.g., `github.com/owner/repo` with version `v2.0.0+incompatible`)

```
detectSubModuleForStandardModule()
  |
  v
subModuleName = getSubModule(projectName)
  |
  v
+-- shouldPackSubModule() (subModuleName is not empty)?
|      |
|     YES --> This is a submodule
|     NO  --> This is a regular root module
```

**Examples:**

| projectName | version | getSubModule result | Final subModuleName | Description |
|---|---|---|---|---|
| `github.com/owner/repo` | `v1.0.0` | `""` | `""` | Root module, no submodule |
| `github.com/owner/repo` | `v0.5.0` | `""` | `""` | Root module v0 |
| `github.com/owner/repo/pkg/utils` | `v1.2.0` | `"pkg/utils"` | `"pkg/utils"` | Submodule |
| `github.com/owner/repo` | `v2.0.0+incompatible` | `""` | `""` | Incompatible v2 (no /v2 in path) |
| `github.com/owner/repo/mymod` | `v0.1.0` | `"mymod"` | `"mymod"` | Submodule in v0/v1 project |

---

## Phase 2: Scanning Entries (`scanEntries`)

After `subModuleName` is determined, the zip is scanned to:

1. **Find the project's go.mod** -- to extract the Go version directive
2. **Find LICENSE files** -- to include in submodule output
3. **Identify other submodules** -- directories with their own go.mod become `excludedDirectories`

```
For each zip entry:
  |
  +-- Entry ends with "/go.mod"?
  |     |
  |     +-- Is it the go.mod of the module we're packing? (isSubModule() == false)
  |     |      --> YES: Extract Go version from it (setGoVersionByModFile)
  |     |      --> NO:  It's another submodule's go.mod --> add parent dir to excludedDirectories
  |     |
  +-- Entry ends with "LICENSE"?
  |     --> Track as potential license path (priority: submodule LICENSE > root LICENSE)
  |
  +-- Otherwise: Track directory for later exclusion filtering
```

### `isSubModule(entryName)` Logic

```
Does entryName end with "/go.mod"?
  |
  NO --> return false (not a module marker)
  |
  YES:
    |
    +-- shouldPackSubModule() (subModuleName is not empty)?
    |      |
    |     YES --> Is it NOT the go.mod of our submodule?
    |            Check: entryName (after root/) does NOT end with subModuleName + "/go.mod"
    |            If NOT our submodule's go.mod --> return true (it's another submodule)
    |            If it IS our submodule's go.mod --> return false
    |     NO  --> Is it NOT the root go.mod?
    |            Check: entryName (after root/) != "go.mod"
    |            If NOT root go.mod --> return true (it's a submodule)
    |            If it IS root go.mod --> return false
```

---

## Phase 3: Writing Entries (`writeEntries`) -- Inclusion/Exclusion

```
For each zip entry:
  |
  +-- Is it a directory? --> SKIP (only files are written)
  +-- Is it a symlink?   --> SKIP (Unix symlinks excluded)
  |
  +-- excludeEntry(entryName)?
  |     |
  |    YES --> SKIP
  |    NO  --> Write with corrected entry name
```

### `excludeEntry(entryName)` Decision Tree

```
excludeEntry(entryName):
  |
  +-- Ends with "/.hg_archival.txt"? --> EXCLUDE
  |
  +-- isVendorPackage(entryName)? --> EXCLUDE
  |
  +-- Has at least one "/" in path (i.e., is in a subdirectory)?
  |     |
  |    YES:
  |     +-- shouldPackSubModule()?
  |     |     |
  |     |    YES:
  |     |     +-- Is file OUTSIDE the submodule directory?
  |     |     |   (rootPath == trimmedPrefix, meaning subModuleName not found in path)
  |     |     |     |
  |     |     |    YES --> EXCLUDE (unless it's the LICENSE file)
  |     |     |    NO  --> Check excludedDirectories below
  |     |     |
  |     |    NO: fall through to excludedDirectories check
  |     |
  |     +-- Is the file's parent directory in excludedDirectories?
  |           |
  |          YES --> EXCLUDE (belongs to another submodule)
  |          NO  --> INCLUDE
  |
  +-- No "/" in path (file at zip root level) --> INCLUDE
```

### `isVendorPackage(entryName)` Decision Tree

```
isVendorPackage(entryName):
  |
  +-- Go version >= 1.24 AND entryName ends with "vendor/modules.txt"?
  |     --> YES: EXCLUDE (new in Go 1.24)
  |
  +-- Does path contain "vendor/" segment?
  |     |
  |    NO  --> NOT a vendor package (INCLUDE)
  |    YES:
  |     +-- After the "vendor/" segment, does the remaining path contain "/"?
  |           (i.e., is there a package subdirectory under vendor/)
  |           |
  |          YES --> IS a vendor package (EXCLUDE)
  |          NO  --> NOT a vendor package (INCLUDE, it's a direct file in vendor/)
```

**Vendor examples:**

| Entry | Go Version | isVendorPackage? | Reason |
|---|---|---|---|
| `root/vendor/foo/foo.go` | any | YES | `foo/foo.go` has "/" after vendor |
| `root/pkg/vendor/foo/bar.go` | any | YES | `foo/bar.go` has "/" after vendor |
| `root/vendor/vendor.go` | any | NO | `vendor.go` has no "/" after vendor |
| `root/vendor/modules.txt` | 1.23 | NO | Pre-1.24 `modules.txt` is kept |
| `root/vendor/modules.txt` | 1.24+ | YES | 1.24+ excludes `vendor/modules.txt` |
| `root/notvendor/file.go` | any | NO | No "vendor/" segment |
| `root/vendor/pkg/file.go` | any | YES | `pkg/file.go` has "/" after vendor |

---

## Entry Name Correction (`getCorrectedEntryName`)

Every included file gets its path rewritten:

```
Original:  {root}/{subModuleName}/{relativePath}
Output:    {projectName}@{version}/{relativePath}

Steps:
1. Strip first path element (root dir from tarball, e.g., "repo-abc123/")
2. If result starts with "{subModuleName}/", strip that prefix too
3. Prepend "{projectName}@{version}/"
```

**Examples:**

| subModuleName | Original Entry | Output Entry |
|---|---|---|
| `""` (root) | `repo-abc123/main.go` | `github.com/owner/repo@v1.0.0/main.go` |
| `""` (root) | `repo-abc123/pkg/util.go` | `github.com/owner/repo@v1.0.0/pkg/util.go` |
| `"submodule"` | `repo-abc123/submodule/handler.go` | `github.com/owner/repo/submodule@v1.0.0/handler.go` |
| `"submodule/v2"` | `repo-abc123/submodule/v2/go.mod` | `github.com/owner/repo/submodule/v2@v2.0.0/go.mod` |
| `"foo"` | `repo-abc123/foo/go.mod` | `github.com/owner/repo/foo/v2@v2.1.0/go.mod` |
| `"contrib/nested1"` | `repo-abc123/contrib/nested1/main.go` | `github.com/.../contrib/nested1/v2@v2.0.0/main.go` |

---

## LICENSE Handling

```
setLicenseFilePath(entryName):
  |
  +-- Does entry end with "LICENSE"?
  |     |
  |    NO --> skip
  |    YES:
  |     +-- shouldPackSubModule() AND entry ends with "{subModuleName}/LICENSE"?
  |     |     --> Use submodule LICENSE (highest priority)
  |     |
  |     +-- Is entry the root LICENSE? (after stripping root dir, equals "LICENSE")
  |           --> Use root LICENSE (fallback)
```

**Priority:**
1. Submodule's own LICENSE (`root/submodule/LICENSE`)
2. Root LICENSE (`root/LICENSE`)

When packing a submodule, the root LICENSE is still included if the submodule doesn't have its own.

---

## Complete End-to-End Examples

### Example 1: Simple Root Module

**Input:** `github.com/owner/hello` @ `v1.0.0`

```
Zip contents (hello-abc123.zip):
  hello-abc123/
    go.mod          (module github.com/owner/hello; go 1.21)
    main.go
    pkg/
      util.go
    LICENSE
```

**Decision path:**
- `subModuleNameExplicitlySet` = false
- `isCompatibleModuleFromV2()` = false (v1)
- `detectSubModuleForStandardModule()`: subModule = "" (no path after owner/hello)
- `shouldPackSubModule()` = false

**Output zip:**
```
github.com/owner/hello@v1.0.0/go.mod
github.com/owner/hello@v1.0.0/main.go
github.com/owner/hello@v1.0.0/pkg/util.go
github.com/owner/hello@v1.0.0/LICENSE
```

---

### Example 2: Root Module with Submodules (pack root only)

**Input:** `github.com/owner/project` @ `v1.1.7`

```
Zip contents:
  project-abc123/
    go.mod          (module github.com/owner/project; go 1.20)
    main.go
    some/
      submodule/
        go.mod      (module github.com/owner/project/some/submodule)
        handler.go
    LICENSE
```

**Decision path:**
- Standard module, subModuleName = ""
- `scanEntries()`: finds `some/submodule/go.mod` -> marks `project-abc123/some/submodule` as excluded

**Output zip (root only, submodule excluded):**
```
github.com/owner/project@v1.1.7/go.mod
github.com/owner/project@v1.1.7/main.go
github.com/owner/project@v1.1.7/LICENSE
```

Note: `some/submodule/` is excluded because it has its own go.mod.

---

### Example 3: Packing a Submodule (v1)

**Input:** `github.com/owner/project/some/submodule` @ `v1.1.7`

```
Zip contents (same zip as Example 2):
  project-abc123/
    go.mod          (module github.com/owner/project)
    main.go
    some/
      submodule/
        go.mod      (module github.com/owner/project/some/submodule)
        handler.go
    LICENSE
```

**Decision path:**
- Standard module, subModuleName = "some/submodule"
- `shouldPackSubModule()` = true
- `scanEntries()`: 
  - `project-abc123/go.mod` is identified as another submodule (root's go.mod != our submodule)
  - `project-abc123/some/submodule/go.mod` is OUR go.mod (not a submodule)

**Output zip (submodule only + root LICENSE):**
```
github.com/owner/project/some/submodule@v1.1.7/go.mod
github.com/owner/project/some/submodule@v1.1.7/handler.go
github.com/owner/project/some/submodule@v1.1.7/LICENSE    (from root)
```

---

### Example 4: Compatible v2 Module at Root

**Input:** `github.com/owner/repo/v2` @ `v2.4.0`

```
Zip contents:
  repo-abc123/
    go.mod          (module github.com/owner/repo/v2; go 1.19)
    main.go
    internal/
      core.go
    LICENSE
```

**Decision path:**
- `isCompatibleModuleFromV2()` = true (v2 + projectName ends with /v2)
- `detectSubModuleForCompatibleV2()`:
  - majorVersion = "v2", subModule = "v2"
  - isNestedSubModule = false (subModule == majorVersion)
  - `hasRootModFileOfCompatibleModuleFromV2("v2")` = true (root go.mod ends with "/v2")
  - Returns immediately: subModuleName stays ""

**Output zip (pack from root):**
```
github.com/owner/repo/v2@v2.4.0/go.mod
github.com/owner/repo/v2@v2.4.0/main.go
github.com/owner/repo/v2@v2.4.0/internal/core.go
github.com/owner/repo/v2@v2.4.0/LICENSE
```

---

### Example 5: Submodule with Physical v2 Directory (Subdirectory Layout)

**Input:** `github.com/dorsJfrog/gosubmodule/submodule/v2` @ `v2.0.0`

```
Zip contents:
  gosubmodule-abc123/
    go.mod          (module github.com/dorsJfrog/gosubmodule)
    submodule/
      v2/
        go.mod      (module github.com/dorsJfrog/gosubmodule/submodule/v2)
        lib.go
    LICENSE
```

**Decision path:**
- `isCompatibleModuleFromV2()` = true (v2 + path ends with /v2)
- `detectSubModuleForCompatibleV2()`:
  - majorVersion = "v2", subModule = "submodule/v2"
  - isNestedSubModule = true ("submodule/v2" != "v2")
  - Skips root go.mod check
  - subModuleName = "submodule/v2"
  - `isSubModuleWithMajorVersion("submodule/v2")` = true
  - `hasModFileAtSubModulePath("submodule/v2")` = **true** (zip has `gosubmodule-abc123/submodule/v2/go.mod`)
  - Does NOT strip: subModuleName remains `"submodule/v2"`

**Output zip:**
```
github.com/dorsJfrog/gosubmodule/submodule/v2@v2.0.0/go.mod
github.com/dorsJfrog/gosubmodule/submodule/v2@v2.0.0/lib.go
github.com/dorsJfrog/gosubmodule/submodule/v2@v2.0.0/LICENSE  (from root)
```

---

### Example 6: Submodule with Branch/Tag Layout (No Physical v2 Directory)

**Input:** `github.com/owner/repo/foo/v2` @ `v2.1.0`

```
Zip contents:
  repo-abc123/
    go.mod          (module github.com/owner/repo)
    foo/
      go.mod        (module github.com/owner/repo/foo/v2; go 1.21)
      handler.go
    LICENSE
```

**Decision path:**
- `isCompatibleModuleFromV2()` = true
- `detectSubModuleForCompatibleV2()`:
  - majorVersion = "v2", subModule = "foo/v2"
  - isNestedSubModule = true ("foo/v2" != "v2")
  - subModuleName = "foo/v2"
  - `isSubModuleWithMajorVersion("foo/v2")` = true
  - `hasModFileAtSubModulePath("foo/v2")` = **false** (no `repo-abc123/foo/v2/go.mod`)
  - Strips: subModuleName = `"foo"`

**Output zip:**
```
github.com/owner/repo/foo/v2@v2.1.0/go.mod
github.com/owner/repo/foo/v2@v2.1.0/handler.go
github.com/owner/repo/foo/v2@v2.1.0/LICENSE  (from root)
```

---

### Example 7: Nested Submodule with Root also v2

**Input:** `github.com/thepudds/nested-module-example/contrib/nested1/v2` @ `v2.0.0`

```
Zip contents:
  nested-module-example-abc123/
    go.mod          (module github.com/thepudds/nested-module-example/v2; go 1.20)
    contrib/
      nested1/
        go.mod      (module github.com/thepudds/nested-module-example/contrib/nested1/v2)
        nested.go
    LICENSE
```

**Decision path:**
- `isCompatibleModuleFromV2()` = true
- `detectSubModuleForCompatibleV2()`:
  - majorVersion = "v2", subModule = "contrib/nested1/v2"
  - isNestedSubModule = true ("contrib/nested1/v2" != "v2")
  - Even though root go.mod declares /v2, we proceed (nested submodule)
  - subModuleName = "contrib/nested1/v2"
  - `isSubModuleWithMajorVersion("contrib/nested1/v2")` = true
  - `hasModFileAtSubModulePath("contrib/nested1/v2")` = **false** (no physical `contrib/nested1/v2/go.mod`)
  - Strips: subModuleName = `"contrib/nested1"`

**Output zip:**
```
github.com/thepudds/nested-module-example/contrib/nested1/v2@v2.0.0/go.mod
github.com/thepudds/nested-module-example/contrib/nested1/v2@v2.0.0/nested.go
github.com/thepudds/nested-module-example/contrib/nested1/v2@v2.0.0/LICENSE  (from root)
```

---

### Example 8: Incompatible v2 Module

**Input:** `github.com/owner/repo` @ `v2.0.0+incompatible`

```
Zip contents:
  repo-abc123/
    go.mod          (module github.com/owner/repo; go 1.11)
    main.go
    LICENSE
```

**Decision path:**
- `isCompatibleModuleFromV2()` = false (projectName doesn't end with /v2)
- `detectSubModuleForStandardModule()`: subModule = "" (root module)
- Pack from root

**Output zip:**
```
github.com/owner/repo@v2.0.0+incompatible/go.mod
github.com/owner/repo@v2.0.0+incompatible/main.go
github.com/owner/repo@v2.0.0+incompatible/LICENSE
```

---

### Example 9: Module with Vendor Directory

**Input:** `github.com/owner/testvendor` @ `v1.0.4`

```
Zip contents:
  testvendor-abc123/
    go.mod          (module github.com/owner/testvendor; go 1.23)
    main.go
    vendor/
      modules.txt
      github.com/
        pkg/
          errors/
            errors.go
      vendor.go
    LICENSE
```

**Decision path:**
- Standard module, pack from root
- Vendor exclusion (Go 1.23):
  - `vendor/modules.txt` -> NOT excluded (Go < 1.24, and it's directly under vendor with no sub-path)
  - Actually `vendor/modules.txt` ends with `vendor/modules.txt` but `substring(i).contains("/")` is checked on the part AFTER `vendor/`. For `vendor/modules.txt`, after the `vendor/` prefix we have `modules.txt` which has no `/`, so NOT excluded.
  - `vendor/github.com/pkg/errors/errors.go` -> IS excluded (`github.com/pkg/errors/errors.go` contains "/")
  - `vendor/vendor.go` -> NOT excluded (`vendor.go` has no "/")

**Output zip:**
```
github.com/owner/testvendor@v1.0.4/go.mod
github.com/owner/testvendor@v1.0.4/main.go
github.com/owner/testvendor@v1.0.4/vendor/modules.txt
github.com/owner/testvendor@v1.0.4/vendor/vendor.go
github.com/owner/testvendor@v1.0.4/LICENSE
```

Note: Vendored packages (anything under `vendor/` with a sub-path) are EXCLUDED.

---

### Example 10: Module with Vendor Directory (Go 1.24+)

**Input:** `github.com/owner/testvendor` @ `v1.0.2`

```
Zip contents:
  testvendor-abc123/
    go.mod          (module github.com/owner/testvendor; go 1.24)
    main.go
    vendor/
      modules.txt
      github.com/
        pkg/
          errors/
            errors.go
      vendor.go
    LICENSE
```

**Decision path:**
- Same as Example 9 but Go version is 1.24
- `vendor/modules.txt` -> IS excluded (Go >= 1.24 special case)
- `vendor/github.com/pkg/errors/errors.go` -> IS excluded (vendor package)
- `vendor/vendor.go` -> NOT excluded

**Output zip:**
```
github.com/owner/testvendor@v1.0.2/go.mod
github.com/owner/testvendor@v1.0.2/main.go
github.com/owner/testvendor@v1.0.2/vendor/vendor.go
github.com/owner/testvendor@v1.0.2/LICENSE
```

---

### Example 11: Multiple Submodules (pack one)

**Input:** `github.com/p4r53c/go-lic-repro/pkg/module1` @ `v0.1.0`

```
Zip contents:
  go-lic-repro-abc123/
    go.mod          (module github.com/p4r53c/go-lic-repro)
    pkg/
      module1/
        go.mod      (module github.com/p4r53c/go-lic-repro/pkg/module1)
        handler.go
      module2/
        go.mod      (module github.com/p4r53c/go-lic-repro/pkg/module2)
        other.go
    LICENSE
    pkg/module1/LICENSE
```

**Decision path:**
- Standard module, subModuleName = "pkg/module1"
- `scanEntries()`:
  - `go-lic-repro-abc123/go.mod` -> excluded (root module != our submodule)
  - `go-lic-repro-abc123/pkg/module1/go.mod` -> OUR go.mod
  - `go-lic-repro-abc123/pkg/module2/go.mod` -> excluded (another submodule)
- LICENSE: submodule has its own (`pkg/module1/LICENSE`), so that takes priority

**Output zip:**
```
github.com/p4r53c/go-lic-repro/pkg/module1@v0.1.0/go.mod
github.com/p4r53c/go-lic-repro/pkg/module1@v0.1.0/handler.go
github.com/p4r53c/go-lic-repro/pkg/module1@v0.1.0/LICENSE
```

---

### Example 12: Explicit SubModule from GitLab (empty = no submodule)

**Input:** `gitlab.com/group/subgroup/project` @ `v1.0.0`
**Fetcher set:** `setSubModuleNameExplicitly("")`

```
Zip contents:
  project-abc123/
    go.mod          (module gitlab.com/group/subgroup/project)
    main.go
```

**Decision path:**
- `subModuleNameExplicitlySet` = true
- `applyExplicitSubModule()`: subModuleName stays ""
- Pack from root (the automatic detection would have incorrectly detected "subgroup" as submodule)

**Output zip:**
```
gitlab.com/group/subgroup/project@v1.0.0/go.mod
gitlab.com/group/subgroup/project@v1.0.0/main.go
```

---

## Summary Table: All Module Types

| Type | Condition | subModuleName | Files Included |
|---|---|---|---|
| Root module (v0/v1) | version < v2, no submodule path | `""` | All root files, excluding other submodules and vendor packages |
| Submodule (v0/v1) | version < v2, has submodule path | `"path/to/sub"` | Files under submodule/ only + LICENSE |
| Compatible v2+ at root | v2+, projectName ends /vN, root go.mod declares /vN | `""` | All root files (same as root module) |
| Submodule v2+ (subdirectory layout) | v2+, nested submodule, physical vN/ dir exists | `"sub/vN"` | Files under sub/vN/ + LICENSE |
| Submodule v2+ (branch/tag layout) | v2+, nested submodule, no physical vN/ dir | `"sub"` (stripped) | Files under sub/ + LICENSE |
| Incompatible v2+ | v2+ but projectName lacks /vN | `""` | All root files (treated as root module) |
| Explicit (from fetcher) | `setSubModuleNameExplicitly()` called | as provided | Depends on value provided |

---

## Files Always Excluded

1. **Symlinks** -- Unix symlinks are always skipped
2. **Directories** -- Only files are written
3. **`.hg_archival.txt`** -- Mercurial metadata
4. **Vendor packages** -- Files in `vendor/` subdirectories (but not direct files like `vendor/vendor.go`)
5. **`vendor/modules.txt`** -- Only in Go >= 1.24
6. **Other submodules' files** -- Any directory containing its own `go.mod` (that isn't the target module)
7. **Files outside submodule** -- When packing a submodule, root files are excluded (except LICENSE)

## Files Always Included

1. **Target module's `go.mod`**
2. **All source files** within the target module's directory tree (that aren't in excluded submodules)
3. **LICENSE** -- Either from the submodule itself or inherited from root
