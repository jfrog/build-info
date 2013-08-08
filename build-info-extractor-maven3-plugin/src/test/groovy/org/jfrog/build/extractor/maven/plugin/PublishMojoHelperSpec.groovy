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
        '{}'                       | '{}'
        '{""}'                     | ''
        '{"abc"}'                  | 'abc'
        '{}{""}'                   | '{}'
        '{""}{}'                   | '{}'
        '{"abc"}def{""}'           | 'abcdef'
        '{"abc"}def{"zzz"}'        | 'abcdefzzz'
        '{A|B|C|"def"}'            | 'def'
        '{A|B|C|def}'              | '{A|B|C|def}'
        '{JAVA_HOME2|EDITOR2}'     | '{JAVA_HOME2|EDITOR2}'
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
