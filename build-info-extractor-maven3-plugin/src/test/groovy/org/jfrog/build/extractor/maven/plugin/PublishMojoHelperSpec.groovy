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
        expression                 | expected
        '{}'                       | null
        '{""}'                     | '{}'
        '{"abc"}'                  | '{abc}'
        '{abc}'                    | null
        '{"abc|def"}'              | '{abc|def}'
        '{}{""}'                   | 'null{}'
        '{""}{}'                   | '{}null'
        '{"abc"}def{""}'           | '{abc}def{}'
        '{"abc"}def{"zzz"}'        | '{abc}def{zzz}'
        '{"abc"}def{"zzz|uuu"}'    | '{abc}def{zzz|uuu}'
        '{"abc|xxx"}def{"zzz|uuu"}'| '{abc|xxx}def{zzz|uuu}'
        '{A|B|C|"def"}'            | 'def'
        '{A|B|C|def}'              | null
        '{A|B}_{D|E}'              | 'null_null'
        '{A|B}_{D|E|"f"}'          | 'null_f'
        '{A|B|"c"}_{D|E}'          | 'c_null'
        '{A|B|"c"}_{D|E|"ee"}'     | 'c_ee'
        '{JAVA_HOME2|EDITOR2}'     | null
        '{JAVA_HOME2|EDITOR2|""}'  | ''
        '{JAVA_HOME2|EDITOR2|"a"}' | 'a'
        '{A|EDITOR2|B|"aa"}'       | 'aa'
    }


    @Unroll( "'#expression' => System.getenv(#variables)" )
    def 'updateValue() - variables' ( String expression, List<String> variables )
    {
        expect:
        helper.updateValue( expression ) == variables.collect { System.getenv( it ) }.join( '|' )

        where:
        expression                 | variables
        '{JAVA_HOME}'              | [ 'JAVA_HOME' ]
        '{A|JAVA_HOME|B}'          | [ 'JAVA_HOME' ]
        '{JAVA_HOME|JAVA_HOME}'    | [ 'JAVA_HOME' ]
        '{AAA|JAVA_HOME}'          | [ 'JAVA_HOME' ]
        '{JAVA_HOME|BBB}'          | [ 'JAVA_HOME' ]
        '{JAVA_HOME}|{JAVA_HOME}'  | [ 'JAVA_HOME', 'JAVA_HOME'  ]
    }
}
