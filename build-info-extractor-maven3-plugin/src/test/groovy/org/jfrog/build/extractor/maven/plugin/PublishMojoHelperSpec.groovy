package org.jfrog.build.extractor.maven.plugin

import spock.lang.Specification
import spock.lang.Unroll


/**
 * {@link PublishMojoHelper} specs.
 */
class PublishMojoHelperSpec extends Specification
{
    final helper = new PublishMojoHelper( new PublishMojo())


    @Unroll( "'#expression' => '#expected'" )
    def 'updateValue() - constants' ( String expression, String expected )
    {
        expect:
        helper.updateValue( expression ) == expected

        where:
        expression                       | expected
        '{{}}'                           | null
        '{{""}}'                         | ''
        '{{"abc"}}'                      | 'abc'
        '{{abc}}'                        | null
        '{{"abc|def"}}'                  | 'abc|def'
        '{{}}{{""}}'                     | null
        '{{""}}{{}}'                     | null
        '{{"abc"}}def{{""}}'             | 'abcdef'
        '{{"abc"}}def{{"zzz"}}'          | 'abcdefzzz'
        '{{"abc"}}def{{"zzz|uuu"}}'      | 'abcdefzzz|uuu'
        '{{"abc|xxx"}}def{{"zzz|uuu"}}'  | 'abc|xxxdefzzz|uuu'
        '{{A|B|C|"def"}}'                | 'def'
        '{{A|B|C|def}}'                  | null
        '{{A|B}}_{{D|E}}'                | 'null_null'
        '{{A|B}}_{{D|E|"f"}}'            | 'null_f'
        '{{A|B|"c"}}_{{D|E}}'            | 'c_null'
        '{{A|B|"c"}}_{{D|E|"ee"}}'       | 'c_ee'
        '{{JAVA_HOME2|EDITOR2}}'         | null
        '{{JAVA_HOME2|EDITOR2|""}}'      | ''
        '{{JAVA_HOME2|EDITOR2|"a"}}'     | 'a'
        '{{A|EDITOR2|B|"aa"}}'           | 'aa'
        'aa{{}}bb'                       | 'aanullbb'
        'aa{{""}}'                       | 'aa'
        'aa{{"abc"}}'                    | 'aaabc'
        'aa{{abc}}'                      | 'aanull'
        'aa{{"abc|def"}}'                | 'aaabc|def'
        'aa{{}}{{""}}'                   | 'aanull'
        'aa{{""}}{{}}'                   | 'aanull'
        'aa{{"abc"}}def{{""}}'           | 'aaabcdef'
        'aa{{"abc"}}def{{"zzz"}}'        | 'aaabcdefzzz'
        'aa{{"abc"}}def{{"zzz|uuu"}}'    | 'aaabcdefzzz|uuu'
        'aa{{"abc|xxx"}}def{{"zzz|uuu"}}'| 'aaabc|xxxdefzzz|uuu'
        'aa{{A|B|C|"def"}}'              | 'aadef'
        'aa{{A|B|C|def}}'                | 'aanull'
        'aa{{A|B}}_{{D|E}}'              | 'aanull_null'
        'aa{{A|B}}_{{D|E|"f"}}'          | 'aanull_f'
        'aa{{A|B|"c"}}_{{D|E}}'          | 'aac_null'
        'aa{{A|B|"c"}}_{{D|E|"ee"}}'     | 'aac_ee'
        'aa{{JAVA_HOME2|EDITOR2}}'       | 'aanull'
        'aa{{JAVA_HOME2|EDITOR2|""}}'    | 'aa'
        'aa{{JAVA_HOME2|EDITOR2|"a"}}'   | 'aaa'
        'aa{{A|EDITOR2|B|"aa"}}'         | 'aaaa'
    }


    @Unroll( "'#expression' => System.getenv(#variables)" )
    def 'updateValue() - variables' ( String expression, List<String> variables )
    {
        expect:
        ( System.getenv( 'JAVA_HOME' ) == null ) || ( helper.updateValue( expression ) == variables.collect { System.getenv( it ) }.join( '|' ))

        where:
        expression                    | variables
        '{{JAVA_HOME}}'               | [ 'JAVA_HOME' ]
        '{{A|JAVA_HOME|B}}'           | [ 'JAVA_HOME' ]
        '{{JAVA_HOME|JAVA_HOME}}'     | [ 'JAVA_HOME' ]
        '{{AAA|JAVA_HOME}}'           | [ 'JAVA_HOME' ]
        '{{JAVA_HOME|BBB}}'           | [ 'JAVA_HOME' ]
        '{{JAVA_HOME}}|{{JAVA_HOME}}' | [ 'JAVA_HOME', 'JAVA_HOME'  ]
    }
}
