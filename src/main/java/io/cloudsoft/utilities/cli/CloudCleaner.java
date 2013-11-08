package io.cloudsoft.utilities.cli;

import io.airlift.command.Cli;
import io.airlift.command.Help;

public class CloudCleaner {

    public static void main(String[] args) {
        Cli.CliBuilder<Runnable> builder = Cli.<Runnable>builder("cloud-cleaner")
                .withDescription("Your cloud assistant")
                .withDefaultCommand(Help.class)
                .withCommands(Help.class, ListInstances.class, DestroyNetworks.class, DestroyNodes.class,
                        CleanCloud.class);
        Cli<Runnable> gitParser = builder.build();
        gitParser.parse(args).run();
    }
}