# Deployment

Classpath should contain at least:

* hazelcast-daemon.jar
* com.hazelcast:hazelcast:[,2.x)
* io.airlift:airline:0.4
* javax.inject:javax.inject:1
* com.google.guava:guava:10.0
* com.google.code.findbugs:jsr305:1.3.9
* (optional) application/domain jars

Or you can use the -jar-with-dependencies uber jar of hazelcast-daemon (note: it includes hazelcast):

    java -jar hazelcast-daemon-0.2-jar-with-dependencies.jar start
    java -jar hazelcast-daemon-0.2-jar-with-dependencies.jar stop

# Usage

    usage: hazelcast-daemon <command> [<args>]

    The most commonly used hazelcast-daemon commands are:
        jvm       Summary of the JVM
        members   Show members of the cluster
        start     Start a hazelcast node
        status    Status of the JVM
        stop      Stop a hazelcast node

    See 'hazelcast-daemon help <command>' for more information on a specific
    command.

## Start

    NAME
            hazelcast-daemon start - Start a hazelcast node

    SYNOPSIS
            hazelcast-daemon start
                    [(--command <shutdown command> | -cmd <shutdown command>)]
                    [(--configuration <configuration> | -c <configuration>)]
                    [(--host <admin host> | -h <admin host>)]
                    [(--name <instance name> | -n <instance name>)]
                    [(--port <admin port> | -p <admin port>)]

    OPTIONS
            --command <shutdown command>, -cmd <shutdown command>
                the shutdown command

            --configuration <configuration>, -c <configuration>
                the path to the hazelcast xml configuration

            --host <admin host>, -h <admin host>
                the host used to listen shutdown command

            --name <instance name>, -n <instance name>
                the hazelcast instance name

            --port <admin port>, -p <admin port>
                the port used to listen shutdown command

## Stop

    NAME
            hazelcast-daemon stop - Stop a hazelcast node

    SYNOPSIS
            hazelcast-daemon stop
                    [(--command <shutdown command> | -cmd <shutdown command>)]
                    [(--host <admin host> | -h <admin host>)]
                    [(--port <admin port> | -p <admin port>)]

    OPTIONS
            --command <shutdown command>, -cmd <shutdown command>
                the shutdown command

            --host <admin host>, -h <admin host>
                the host used to listen shutdown command

            --port <admin port>, -p <admin port>
                the port used to listen shutdown command

## Other commands configuration

In the following help snippet X can be [jvm, members, status].

    NAME
            hazelcast-daemon X - Summary of the JVM

    SYNOPSIS
            hazelcast-daemon X [(--host <admin host> | -h <admin host>)]
                    [(--port <admin port> | -p <admin port>)]

    OPTIONS
            --host <admin host>, -h <admin host>
                the host used to listen shutdown command

            --port <admin port>, -p <admin port>
                the port used to listen shutdown command