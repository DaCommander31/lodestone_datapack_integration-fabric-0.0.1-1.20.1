package net.dacommander31.lodestone_di.integration;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LodestoneJsonPathArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList(
            "namespace:json_file_name", "ldi:example"
    );

    public LodestoneJsonPathArgumentType() {
    }

    public static LodestoneJsonPathArgumentType jsonPath() {
        return new LodestoneJsonPathArgumentType();
    }

    public static String getJsonPath(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        StringBuilder builder = new StringBuilder();
        boolean colonFound = false;

        while (reader.canRead()) {
            char c = reader.peek();

            // Stop parsing if a space is encountered (indicates the next parameter)
            if (Character.isWhitespace(c)) {
                break;
            }

            // Colon, slash, and underscore are allowed, so it's safe to process them in paths
            if (Character.isLetterOrDigit(c) || c == ':' || c == '_' || c == '/') {
                if (c == ':') {
                    if (colonFound) {
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
                    }
                    colonFound = true;  // We allow only one colon for namespace
                }
                builder.append(c);
                reader.skip();
            } else {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
            }
        }

        if (builder.length() == 0 || !colonFound) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
        }

        return builder.toString();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static CompletableFuture<Suggestions> suggestJsonPaths(ServerCommandSource source, SuggestionsBuilder builder) {
        List<String> suggestions = new ArrayList<>();
        try {
            // Access the datapacks folder using WorldSavePath.DATAPACKS
            Path datapacksFolder = source.getServer().getSavePath(WorldSavePath.DATAPACKS);

            if (Files.exists(datapacksFolder)) {

                Files.newDirectoryStream(datapacksFolder).forEach(datapackFolder -> {

                    Path dataFolder = datapackFolder.resolve("data");

                    if (Files.exists(dataFolder)) {

                        // Traverse all namespaces and folders within each datapack's data folder
                        try {
                            Files.walk(dataFolder)
                                    .filter(Files::isRegularFile)
                                    .filter(path -> path.toString().endsWith(".json"))
                                    .forEach(path -> {
                                        String relativePath = dataFolder.relativize(path).toString().replace("\\", "/");
                                        String[] pathParts = relativePath.split("/", 2);
                                        if (pathParts.length >= 2 && pathParts[1].startsWith("lodestone/particle/")) {
                                            String namespace = pathParts[0];
                                            String pathWithoutPrefix = relativePath.replaceFirst("^" + pathParts[0] + "/lodestone/particle/", "");
                                            String jsonFileName = pathWithoutPrefix.replace(".json", "");
                                            suggestions.add(namespace + ":" + jsonFileName);
                                        }
                                    });

                        } catch (IOException ignore) {

                        }
                    }
                });
            }

        } catch (Exception ignore) {

        }

        // Add suggestions to the builder and return them
        for (String suggestion : suggestions) {
            builder.suggest(suggestion);
        }

        return builder.buildFuture();  // Return the suggestions asynchronously
    }
}
