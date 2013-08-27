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
        '{{abc}}'                        | null
        '{{"abc|def"}}'                  | null
        '{{A|B|C|def}}'                  | null
        '{{""}}'                         | ''
        '{{"abc"}}'                      | 'abc'
        '{{"abc|"def"}}'                 | 'def'
        '{{abc|"def"}}'                  | 'def'
        '{{}}{{""}}'                     | ''
        '{{""}}{{}}'                     | ''
        '{{"abc"}}def{{""}}'             | 'abcdef'
        '{{"abc"}}def{{"zzz"}}'          | 'abcdefzzz'
        '{{"abc"}}def{{"zzz|uuu"}}'      | 'abcdef'
        '{{"abc|xxx"}}def{{"zzz|uuu"}}'  | 'def'
        '{{A|B|C|"def"}}'                | 'def'
        '{{A|B}}_{{D|E}}'                | '_'
        '{{A|B}}_{{D|E|"f"}}'            | '_f'
        '{{A|B|"c"}}_{{D|E}}'            | 'c_'
        '{{A|B|"c"}}_{{D|E|"ee"}}'       | 'c_ee'
        '{{JAVA_HOME2|EDITOR2}}'         | null
        '{{JAVA_HOME2|EDITOR2|""}}'      | ''
        '{{JAVA_HOME2|EDITOR2|"a"}}'     | 'a'
        '{{A|EDITOR2|B|"aa"}}'           | 'aa'
        'aa{{}}bb'                       | 'aabb'
        'aa{{""}}'                       | 'aa'
        'aa{{"abc"}}'                    | 'aaabc'
        'aa{{abc}}'                      | 'aa'
        'aa{{"abc|def"}}'                | 'aa'
        'aa{{"abc|"def"}}'               | 'aadef'
        'aa{{abc|"def"}}'                | 'aadef'
        'aa{{}}{{""}}'                   | 'aa'
        'aa{{""}}{{}}'                   | 'aa'
        'aa{{"abc"}}def{{""}}'           | 'aaabcdef'
        'aa{{"abc"}}def{{""}}{{qqq}}'    | 'aaabcdef'
        'aa{{"abc"}}def{{"zzz"}}'        | 'aaabcdefzzz'
        'aa{{"abc"}}def{{"zzz|uuu"}}'    | 'aaabcdef'
        'aa{{"abc|xxx"}}def{{"zzz|uuu"}}'| 'aadef'
        'aa{{A|B|C|"def"}}'              | 'aadef'
        'aa{{A|B|C|def}}'                | 'aa'
        'aa{{A|B}}_{{D|E}}'              | 'aa_'
        'aa{{A|B}}_{{D|E|"f"}}'          | 'aa_f'
        'aa{{A|B|"c"}}_{{D|E}}'          | 'aac_'
        'aa{{A|B|"c"}}_{{D|E|"ee"}}'     | 'aac_ee'
        'aa{{JAVA_HOME2|EDITOR2}}'       | 'aa'
        'aa{{JAVA_HOME2|EDITOR2|""}}'    | 'aa'
        'aa{{JAVA_HOME2|EDITOR2|"x"}}'   | 'aax'
        'aa{{A|EDITOR2|B|"rr"}}'         | 'aarr'
        'aa{{A|C|B|"rr"}}z{ff}{{d}}'     | 'aarrz{ff}'
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
