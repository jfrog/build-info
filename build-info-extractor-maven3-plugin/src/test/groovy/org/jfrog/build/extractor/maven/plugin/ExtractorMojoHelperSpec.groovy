package org.jfrog.build.extractor.maven.plugin

import spock.lang.Specification
import spock.lang.Unroll


/**
 * {@link ExtractorMojoHelper} specs.
 */
class ExtractorMojoHelperSpec extends Specification
{
    final helper = new ExtractorMojoHelper( new ExtractorMojo())


    @Unroll( "'#expression' => '#expected'" )
    def 'updateValue() - constants' ( String expression, String expected )
    {
        expect:
        helper.updateValue( expression ) == expected

        where:
        expression                 | expected
        '{}'                       | 'null'
        '{""}'                     | ''
        '{"abc"}'                  | 'abc'
        '{}{""}'                   | 'null'
        '{""}{}'                   | 'null'
        '{"abc"}def{""}'           | 'abcdef'
        '{"abc"}def{"zzz"}'        | 'abcdefzzz'
        '{A|B|C|"def"}'            | 'def'
        '{A|B|C|def}'              | 'null'
        '{JAVA_HOME2|EDITOR2}'     | 'null'
        '{JAVA_HOME2|EDITOR2|""}'  | ''
        '{JAVA_HOME2|EDITOR2|"a"}' | 'a'
        '{A|EDITOR2|B|"aa"}'       | 'aa'
    }


    @Unroll( "'#expression' => System.getenv(#variables)" )
    def 'updateValue() - variables' ( String expression, List<String> variables )
    {
        expect:
        helper.updateValue( expression ) == variables.collect { String.valueOf( System.getenv( it )) }.join( '|' )

        where:
        expression               | variables

        '{JAVA_HOME}'            | [ 'JAVA_HOME' ]
        '{EDITOR}'               | [ 'EDITOR' ]
        '{JAVA_HOME2}'           | [ 'JAVA_HOME2' ]
        '{EDITOR2}'              | [ 'EDITOR2' ]
        '{A|JAVA_HOME|B}'        | [ 'JAVA_HOME' ]
        '{A|EDITOR|B}'           | [ 'EDITOR' ]
        '{A|JAVA_HOME2|B}'       | [ 'JAVA_HOME2' ]
        '{A|EDITOR2|B}'          | [ 'EDITOR2' ]

        '{JAVA_HOME|EDITOR}'     | [ 'JAVA_HOME' ]
        '{JAVA_HOME2|EDITOR}'    | [ 'EDITOR'  ]
        '{JAVA_HOME|EDITOR2}'    | [ 'JAVA_HOME' ]
        '{JAVA_HOME2|EDITOR2}'   | [ 'EDITOR2' ]

        '{JAVA_HOME}|{EDITOR}'   | [ 'JAVA_HOME',  'EDITOR'  ]
        '{JAVA_HOME2}|{EDITOR}'  | [ 'JAVA_HOME2', 'EDITOR'  ]
        '{JAVA_HOME}|{EDITOR2}'  | [ 'JAVA_HOME',  'EDITOR2' ]
        '{JAVA_HOME2}|{EDITOR2}' | [ 'JAVA_HOME2', 'EDITOR2' ]

    }
}
