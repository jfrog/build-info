# Go Test Zip Fixtures

Each `test-*.zip` is a source zipball (simulating a GitHub/GitLab tarball download).
Each `res-*.zip` is the expected output after GoZipBallStreamer processes the source.

## Fixture Layouts

### test-submodule-v2.zip
- Layout: `{root}/submodule/v2/go.mod` (subdirectory layout)
- Module: `github.com/dorsJfrog/gosubmodule/submodule/v2`
- The `/v2` path is a physical directory containing its own `go.mod`.

### test-submodule-v2-branch-layout.zip
- Layout: `{root}/foo/go.mod` (branch/tag layout, no physical `v2/` directory)
- Module: `github.com/owner/repo/foo/v2`
- The `/v2` in the module path is a major version indicator only; `go.mod` lives at `foo/go.mod`.

### test-nested-module-v2.zip
- Layout: `{root}/contrib/nested1/v2/go.mod` (subdirectory layout for nested submodule)
- Module: `github.com/thepudds/nested-module-example/contrib/nested1/v2`
- Physical `contrib/nested1/v2/` directory exists.

### test-nested-submodule-v2-root-v2.zip
- Layout: `{root}/go.mod` declares module `/v2` at root; no `contrib/nested1/v2/go.mod`
- Module (root): `github.com/thepudds/nested-module-example/v2`
- Module (nested): `github.com/thepudds/nested-module-example/contrib/nested1/v2`
- Tests that nested submodule detection works even when root go.mod also has `/v2`.
- The nested submodule uses branch/tag layout (go.mod at `contrib/nested1/go.mod`).

### test-modulesLIC.zip
- Contains multiple submodules (`pkg/module1`, `pkg/module2`) with LICENSE files at various levels.

### versionsTest-master.zip
- Contains root `go.mod` with `/v2` compatible module naming.
- Used to test compatible version detection for both v1 and v2 modules.

### excluded-files-test-v1.zip
- Contains files that should be excluded per Go zip spec (symlinks, special files).

### project-with-submodule.zip
- Simple project with a `some/submodule` subdirectory.

### complex-project.zip
- Multi-module project with `firstModule` and `secondModule` submodules.

### test-simple-root-module.zip
- Layout: `{root}/go.mod`, `{root}/main.go`, `{root}/pkg/util.go`, `{root}/LICENSE`
- Module: `github.com/owner/hello` @ `v1.0.0`
- Simple root module with no submodules at all. Tests the basic root-module path
  with zero `excludedDirectories`.

### test-incompatible-v2.zip
- Layout: `{root}/go.mod` (`module github.com/owner/repo`, go 1.11), `{root}/main.go`, `{root}/LICENSE`
- Module: `github.com/owner/repo` @ `v2.0.0+incompatible`
- Incompatible v2 module: major version ≥ 2 but project name lacks `/v2`, so
  `isCompatibleGoModuleNaming` returns false and the standard-module path is used.

### test-gopkg-in-v2.zip
- Layout: `{root}/go.mod` (`module gopkg.in/yaml.v2`, go 1.15), `{root}/yaml.go`, `{root}/decode.go`, `{root}/LICENSE`
- Module: `gopkg.in/yaml.v2` @ `v2.4.0`
- gopkg.in uses dot notation (`.v2`) not slash (`/v2`). Despite being at major version 2,
  `isCompatibleGoModuleNaming` returns false and the module is packed from root as a
  standard module. Validates that the zip prefix uses `gopkg.in/yaml.v2@v2.4.0/`.
