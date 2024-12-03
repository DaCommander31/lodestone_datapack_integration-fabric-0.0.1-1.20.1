package net.dacommander31.lodestone_di.integration;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class LodestoneParticleArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList(
            "lodestone:wisp", "lodestone:smoke", "lodestone:spark", "lodestone:twinkle"
    );

    public LodestoneParticleArgumentType() {
    }

    public static LodestoneParticleArgumentType particleName() {
        return new LodestoneParticleArgumentType();
    }

    public static String getParticleName(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        StringBuilder builder = new StringBuilder();
        while (reader.canRead()) {
            char c = reader.peek();

            // Stop parsing if a space is encountered (indicates the next parameter)
            if (Character.isWhitespace(c)) {
                break;
            }

            // Allow colons and underscores
            if (Character.isLetterOrDigit(c) || c == ':' || c == '_') {
                builder.append(c);
                reader.skip();
            } else {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
            }
        }

        if (builder.length() == 0) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
        }

        return builder.toString();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        // Add suggestions for the specified particle types
        builder.suggest("lodestone:wisp");
        builder.suggest("lodestone:smoke");
        builder.suggest("lodestone:spark");
        builder.suggest("lodestone:extruding_spark");
        builder.suggest("lodestone:thin_extruding_spark");
        builder.suggest("lodestone:sparkle");
        builder.suggest("lodestone:star");
        builder.suggest("lodestone:twinkle");

        return builder.buildFuture();
    }
}
