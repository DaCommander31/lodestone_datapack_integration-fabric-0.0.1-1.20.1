package net.dacommander31.lodestone_di.integration;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.dacommander31.lodestone_di.LodestoneDatapackIntegration;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class LodestoneDatapackInterpreter {
    private static final Gson GSON = new Gson();

    private static PlayerEntity player = null;
    private static World world;

    public static void initialize() {
        ArgumentTypeRegistry.registerArgumentType(
                new Identifier(LodestoneDatapackIntegration.MOD_ID, "lodestone_particle"),
                LodestoneParticleArgumentType.class,
                ConstantArgumentSerializer.of(LodestoneParticleArgumentType::new)
        );

        ArgumentTypeRegistry.registerArgumentType(
                new Identifier(LodestoneDatapackIntegration.MOD_ID, "lodestone_json_path"),
                LodestoneJsonPathArgumentType.class,
                ConstantArgumentSerializer.of(LodestoneJsonPathArgumentType::new)
        );

        ClientTickEvents.START_CLIENT_TICK.register(minecraftClient -> {
            player = minecraftClient.player;
            }
        );
        ClientTickEvents.START_WORLD_TICK.register((clientWorld) -> {
            if (player != null) {
                world = player.getWorld();
                }
        });

        // Registering command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerLodestoneCommand(dispatcher));

        // Register for datapack reload
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, var) -> reloadJsonPathSuggestions(server.getCommandSource()));
    }

    private static void registerLodestoneCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("lodestoneparticle")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("name", LodestoneParticleArgumentType.particleName())
                                .then(argument("json_path", LodestoneJsonPathArgumentType.jsonPath())
                                        .suggests((context, builder) -> LodestoneJsonPathArgumentType.suggestJsonPaths(context.getSource(), builder))
                                        .then(argument("pos", Vec3ArgumentType.vec3())
                                                .executes(
                                                        context -> execute(
                                                                context,
                                                                LodestoneParticleArgumentType.getParticleName(context, "name"),
                                                                LodestoneJsonPathArgumentType.getJsonPath(context, "json_path"),
                                                                Vec3ArgumentType.getVec3(context, "pos"),
                                                                Vec3d.ZERO,
                                                                0.0f,
                                                                0,
                                                                context.getSource().getServer().getPlayerManager().getPlayerList()
                                                        )
                                                )
                                        ) // Original method
                                                .then(argument("delta", Vec3ArgumentType.vec3())
                                                        .then(argument("speed", FloatArgumentType.floatArg(0.0f))
                                                                .then(argument("count", IntegerArgumentType.integer(0)))
                                                                        .executes(context -> execute(
                                                                                        context,
                                                                                        LodestoneParticleArgumentType.getParticleName(context, "name"),
                                                                                        LodestoneJsonPathArgumentType.getJsonPath(context, "json_path"),
                                                                                        Vec3ArgumentType.getVec3(context, "pos"),
                                                                                        Vec3ArgumentType.getVec3(context, "delta"),
                                                                                        FloatArgumentType.getFloat(context, "speed"),
                                                                                        IntegerArgumentType.getInteger(context, "count"),
                                                                                        context.getSource().getServer().getPlayerManager().getPlayerList()
                                                                                )
                                                                        )
                                                        )
                                                )
                                                                        .then(argument("viewers", EntityArgumentType.players())
                                                                                .executes(
                                                                                        context -> execute(
                                                                                                context,
                                                                                                LodestoneParticleArgumentType.getParticleName(context, "name"),
                                                                                                LodestoneJsonPathArgumentType.getJsonPath(context, "json_path"),
                                                                                                Vec3ArgumentType.getVec3(context, "pos"),
                                                                                                Vec3ArgumentType.getVec3(context, "delta"),
                                                                                                FloatArgumentType.getFloat(context, "speed"),
                                                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                                                EntityArgumentType.getPlayers(context, "viewers")
                                                                                        )
                                                                                )
                                                                        )
                                )
                        )
        );
        dispatcher.register(
                literal("lsp")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("name", LodestoneParticleArgumentType.particleName())
                                .then(argument("json_path", LodestoneJsonPathArgumentType.jsonPath())
                                        .suggests((context, builder) -> LodestoneJsonPathArgumentType.suggestJsonPaths(context.getSource(), builder))
                                        .then(argument("pos", Vec3ArgumentType.vec3())
                                                .executes(
                                                        context -> execute(
                                                                context,
                                                                LodestoneParticleArgumentType.getParticleName(context, "name"),
                                                                LodestoneJsonPathArgumentType.getJsonPath(context, "json_path"),
                                                                Vec3ArgumentType.getVec3(context, "pos"),
                                                                Vec3d.ZERO,
                                                                0.0f,
                                                                0,
                                                                context.getSource().getServer().getPlayerManager().getPlayerList()
                                                        )
                                                )
                                        ) // Original method
                                        .then(argument("delta", Vec3ArgumentType.vec3())
                                                .then(argument("speed", FloatArgumentType.floatArg(0.0f))
                                                        .then(argument("count", IntegerArgumentType.integer(0)))
                                                        .executes(context -> execute(
                                                                        context,
                                                                        LodestoneParticleArgumentType.getParticleName(context, "name"),
                                                                        LodestoneJsonPathArgumentType.getJsonPath(context, "json_path"),
                                                                        Vec3ArgumentType.getVec3(context, "pos"),
                                                                        Vec3ArgumentType.getVec3(context, "delta"),
                                                                        FloatArgumentType.getFloat(context, "speed"),
                                                                        IntegerArgumentType.getInteger(context, "count"),
                                                                        context.getSource().getServer().getPlayerManager().getPlayerList()
                                                                )
                                                        )
                                                )
                                        )
                                        .then(argument("viewers", EntityArgumentType.players())
                                                .executes(
                                                        context -> execute(
                                                                context,
                                                                LodestoneParticleArgumentType.getParticleName(context, "name"),
                                                                LodestoneJsonPathArgumentType.getJsonPath(context, "json_path"),
                                                                Vec3ArgumentType.getVec3(context, "pos"),
                                                                Vec3ArgumentType.getVec3(context, "delta"),
                                                                FloatArgumentType.getFloat(context, "speed"),
                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                EntityArgumentType.getPlayers(context, "viewers")
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context, String name, String jsonPath, Vec3d pos, Vec3d delta, float speed, int count, Collection<ServerPlayerEntity> viewers) {
        ServerCommandSource source = context.getSource();
        try {

            // Parse JSON path
            String[] parts = jsonPath.split(":", 2);
            if (parts.length != 2) {
                source.sendError(Text.literal("Invalid JSON path format. Expected 'namespace:json_file_name'."));
                return 0;
            }

            String namespace = parts[0];
            String fileName = parts[1] + ".json";

            // Load JSON data
            JsonObject jsonData = loadJsonData(namespace + ":" + fileName, source);
            if (jsonData == null) {
                return 0; // Return failure if JSON loading fails
            }

            // Spawn the particle using the parsed JSON
            spawnParticleFromJson(source, pos, jsonData, name);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error processing command: " + e.getMessage()));
            return 0;
        }
    }




    // Reload suggestions for JSON paths during datapack reload
    private static void reloadJsonPathSuggestions(ServerCommandSource source) {
        LodestoneJsonPathArgumentType.suggestJsonPaths(source, new SuggestionsBuilder("", 0));
    }

    private static JsonObject loadJsonData(String path, ServerCommandSource source) {
        String[] parts = path.split(":");
        if (parts.length != 2) {
            source.sendError(Text.literal("Invalid path format. Use 'namespace:particle_name'"));
            return null;
        }

        String namespace = parts[0];
        String particleName = parts[1].replace(".json", ""); // Ensure no double ".json" extension

        // Make sure to resolve the datapacks directory and prevent scanning the entire file system.
        Path datapacksFolder = source.getServer().getSavePath(WorldSavePath.DATAPACKS);

        // Find the datapack for the namespace
        String datapackName = findDatapackName(source, namespace);
        if (datapackName == null) {
            return null;
        }

        // Resolve the path correctly within the datapack folder
        Path jsonFilePath = datapacksFolder
                .resolve(datapackName)                // Ensure we are inside the datapack folder
                .resolve("data")
                .resolve(namespace)
                .resolve("lodestone")
                .resolve("particle")
                .resolve(particleName + ".json");

        try {
            if (Files.exists(jsonFilePath)) {
                String jsonContent = Files.readString(jsonFilePath); // Read the content of the JSON file
                JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject(); // Parse JSON content
                return jsonObject;
            } else {
                source.sendError(Text.literal("JSON file not found at: " + jsonFilePath));
                return null;
            }
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to load JSON from path: " + jsonFilePath + " - " + e.getMessage()));
            return null;
        } catch (JsonSyntaxException e) {
            source.sendError(Text.literal("Invalid JSON format in file: " + jsonFilePath));
            return null;
        }
    }




    public static String findDatapackName(ServerCommandSource source, String namespace) {
        Path saveDir = source.getServer().getSavePath(WorldSavePath.ROOT);
        Path datapacksFolder = saveDir.resolve("datapacks");

        try {
            for (Path datapackFolder : Files.newDirectoryStream(datapacksFolder)) {
                Path dataFolder = datapackFolder.resolve("data").resolve(namespace);
                if (Files.exists(dataFolder)) {
                    return datapackFolder.getFileName().toString();
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static void spawnParticleFromJson(ServerCommandSource source, Vec3d spawnPos, JsonObject jsonData, String particleType) {
        boolean forceSpawn = false;
        boolean culling = false;
        Color startColor;
        Color endColor;
        float colorCoefficient = 1f;
        Easing colorEasingType = Easing.LINEAR;
        float startTransparency;
        float middleTransparency;
        float endTransparency;
        float startScale;
        float middleScale;
        float endScale;

        JsonObject spinData;
        float startSpin;
        float endSpin;
        float spinCoefficient = 1f;
        float worldTimeMultiplier = 1f;
        float spinDividend = 1f;
        Easing spinEasingType = Easing.LINEAR;

        JsonObject transparencyData;
        JsonObject scaleData;

        JsonObject behavior;
        int lifetime = 50;
        int lifeDelay = 0;
        boolean noClip = true;
        JsonObject motionData = null;
        Vec3d motion = Vec3d.ZERO;

        RegistryObject<LodestoneWorldParticleType> particleTypeInstance = getParticleType(particleType);
        if (particleTypeInstance == null) {
            source.sendError(Text.literal("Invalid particle type: " + particleType));
            return;
        }

        WorldParticleBuilder builder =  WorldParticleBuilder.create(particleTypeInstance);

        JsonObject visualData = jsonData.getAsJsonObject("visual_data");
        if (visualData != null) {
            if (visualData.has("force_spawn")) {
                forceSpawn = visualData.get("force_spawn").getAsBoolean();
            }
            if (visualData.has("should_cull")) {
                culling = visualData.get("should_cull").getAsBoolean();
            }
            builder.setForceSpawn(forceSpawn).setShouldCull(culling);
            JsonObject color = visualData.getAsJsonObject("color");
            if (color != null) {
                if (color.has("start_color")) {

                }
                if (color.has("end_color")) {
                endColor = parseColor(color.getAsJsonObject("end_color"));
                }
                if (color.has("easing")) {
                    colorEasingType = getEasingType(color.get("easing").getAsString());
                    if (colorEasingType == null) {
                        source.sendError(Text.literal("Failed to display particle. Invalid easing type provided in \"easing\"."));
                    }
                }
                if (color.has("coefficient")) {
                    colorCoefficient = color.get("coefficient").getAsFloat();
                }

                if (color.has("start_color")) {
                    startColor = parseColor(color.getAsJsonObject("start_color"));

                    if (color.has("end_color")) {
                        endColor = parseColor(color.getAsJsonObject("end_color"));
                        builder.setColorData(ColorParticleData.create(startColor, endColor).setCoefficient(colorCoefficient).setEasing(colorEasingType).build());
                    } else {
                        builder.setColorData(ColorParticleData.create(startColor).setCoefficient(colorCoefficient).setEasing(colorEasingType).build());
                    }
                } else {
                    source.sendError(Text.literal("Failed to display particle. Missing \"start_color\" in \"color\"."));
                    return;
                }

            }

            spinData = visualData.getAsJsonObject("spin");
            if (spinData != null) {
                if (spinData.has("coefficient")) {
                    spinCoefficient = spinData.get("coefficient").getAsFloat();
                }
                if (spinData.has("spin_offset_type")) {
                    if (spinData.get("spin_offset_type").getAsString().equals("world_time")) {
                        if (spinData.has("world_time_multiplier")) {
                            spinDividend = spinData.get("world_time_multiplier").getAsFloat();
                        }

                        if (spinData.has("dividend")) {
                            spinDividend = spinData.get("dividend").getAsFloat();
                        }

                        if (spinData.has("easing")) {
                            spinEasingType = getEasingType(spinData.get("easing").getAsString());
                        }


                        if (spinData.has("start")) {
                            startSpin = spinData.get("start").getAsFloat();
                            if (spinData.has("end")) {
                                endSpin = spinData.get("end").getAsFloat();
                                builder.setSpinData(SpinParticleData.create(startSpin, endSpin).setSpinOffset((source.getWorld().getTime() * worldTimeMultiplier) % spinDividend).setEasing(spinEasingType).build());
                            } else {
                                builder.setSpinData(SpinParticleData.create(startSpin).setSpinOffset((source.getWorld().getTime() * worldTimeMultiplier) % spinDividend).setCoefficient(spinCoefficient).setEasing(spinEasingType).build());
                            }
                        }
                    } else if (spinData.get("spin_offset_type").getAsString().equals("random")) {
                        if (spinData.has("start")) {
                            startSpin = spinData.get("start").getAsFloat();
                            if (spinData.has("end")) {
                                endSpin = spinData.get("end").getAsFloat();
                                builder.setSpinData(SpinParticleData.create(startSpin, endSpin).randomSpinOffset(Random.create()).setEasing(spinEasingType).build());
                            } else {
                                builder.setSpinData(SpinParticleData.create(startSpin).randomSpinOffset(Random.create()).setCoefficient(spinCoefficient).setEasing(spinEasingType).build());
                            }
                        }
                    } else {
                        source.sendError(Text.literal("Failed to display particle. Missing \"spin_offset_type\" in \"spin\"."));
                        return;
                    }

                } else {
                    source.sendError(Text.literal("Failed to display particle. Missing \"start\" in \"spin\"."));
                    return;
                }
            }

            transparencyData = visualData.getAsJsonObject("transparency");
            scaleData = visualData.getAsJsonObject("scale");


            switch (transparencyData.getAsJsonObject().size()) {
                case 1:
                    if (transparencyData.get("start") != null) {
                        startTransparency = transparencyData.get("start").getAsFloat();
                        builder.setTransparencyData(GenericParticleData.create(startTransparency).build());
                    }
                case 2:
                    if (transparencyData.get("start") != null && transparencyData.get("end") != null) {
                        startTransparency = transparencyData.get("start").getAsFloat();
                        endTransparency = transparencyData.get("end").getAsFloat();
                        builder.setTransparencyData(GenericParticleData.create(startTransparency, endTransparency).build());
                    }
                case 3:
                    if (transparencyData.get("start") != null && transparencyData.get("middle") != null && transparencyData.get("end") != null) {
                        startTransparency = transparencyData.get("start").getAsFloat();
                        middleTransparency = transparencyData.get("middle").getAsFloat();
                        endTransparency = transparencyData.get("end").getAsFloat();
                        builder.setTransparencyData(GenericParticleData.create(startTransparency, middleTransparency, endTransparency).build());
                    }
            }

            switch (scaleData.getAsJsonObject().size()) {
                case 1:
                    startScale = scaleData.get("start").getAsFloat();
                    if (scaleData.get("start") != null) {
                        builder.setScaleData(GenericParticleData.create(startScale).build());
                    }
                case 2:
                    startScale = scaleData.get("start").getAsFloat();
                    endScale = scaleData.get("end").getAsFloat();
                    if (scaleData.get("start") != null && scaleData.get("end") != null) {
                        builder.setTransparencyData(GenericParticleData.create(startScale, endScale).build());
                    }
                case 3:
                    startScale = scaleData.get("start").getAsFloat();
                    middleScale = scaleData.get("middle").getAsFloat();
                    endScale = scaleData.get("end").getAsFloat();
                    if (scaleData.get("start") != null && scaleData.get("middle") != null && scaleData.get("end") != null) {
                        builder.setScaleData(GenericParticleData.create(startScale, middleScale, endScale).build());
                    }
            }
        } else {
            source.sendError(Text.literal("Failed to display particle. Provided JSON file missing object \"visual_data\"."));
            return;
        }
        if (jsonData.has("behavior")) {
            behavior = jsonData.getAsJsonObject("behavior");
            if (behavior.has("lifetime")) {
                lifetime = behavior.get("lifetime").getAsInt();
            }
            if (behavior.has("lifetime_delay")) {
                lifeDelay = behavior.get("lifetime_delay").getAsInt();
            }
            if (behavior.has("noclip")) {
                noClip = behavior.get("noclip").getAsBoolean();
            }
            if (behavior.has("motion")) {
                motionData = behavior.getAsJsonObject("motion");
                motion = parseMotion(motionData);
            }

            builder.setLifetime(lifetime).setLifeDelay(lifeDelay);

            if (noClip) {
                builder.enableNoClip();
            }

            if (motionData != null && motionData.has("operation")) {
                String operation = motionData.get("operation").getAsString();
                if (operation.equals("set")) {
                    builder.setMotion(motion.x, motion.y, motion.z);
                } else if (operation.equals("add")) {
                    builder.addMotion(motion.x, motion.y, motion.z);
                } else {
                    source.sendError(Text.literal("Invalid motion operation provided in \"operation\": " + operation));
                    return;
                }
            }
        }

        builder.spawn(world, spawnPos.x, spawnPos.y, spawnPos.z);
        source.sendFeedback(() -> Text.literal("Displaying Lodestone particle " + particleType), true);
    }

    private static Color parseColor(JsonObject colorData) {
        return new Color(
                colorData.get("r").getAsInt(),
                colorData.get("g").getAsInt(),
                colorData.get("b").getAsInt()
        );
    }

    private static Vec3d parseMotion(JsonObject motionData) {
        return new Vec3d(
                motionData.get("x").getAsDouble(),
                motionData.get("y").getAsDouble(),
                motionData.get("z").getAsDouble()
        );
    }

    private static RegistryObject<LodestoneWorldParticleType> getParticleType(String particleType) {
        return switch (particleType) {
            case "wisp", "lodestone:wisp" -> LodestoneParticleRegistry.WISP_PARTICLE;
            case "smoke", "lodestone:smoke" -> LodestoneParticleRegistry.SMOKE_PARTICLE;
            case "spark","lodestone:spark" -> LodestoneParticleRegistry.SPARK_PARTICLE;
            case "extruding_spark", "lodestone:extruding_spark" -> LodestoneParticleRegistry.EXTRUDING_SPARK_PARTICLE;
            case "thin_extruding_spark", "lodestone:thin_extruding_spark" -> LodestoneParticleRegistry.THIN_EXTRUDING_SPARK_PARTICLE;
            case "sparkle", "lodestone:sparkle" -> LodestoneParticleRegistry.SPARKLE_PARTICLE;
            case "star", "lodestone:star" -> LodestoneParticleRegistry.STAR_PARTICLE;
            case "twinkle", "lodestone:twinkle" -> LodestoneParticleRegistry.TWINKLE_PARTICLE;
            default -> null;
        };
    }

    public static Easing getEasingType(String jsonEasingName) {
        return switch (jsonEasingName.toLowerCase()) {
            case "linear" -> Easing.LINEAR;
            case "quad_in" -> Easing.QUAD_IN;
            case "quad_out" -> Easing.QUAD_OUT;
            case "quad_in_out" -> Easing.QUAD_IN_OUT;
            case "cubic_in" -> Easing.CUBIC_IN;
            case "cubic_out" -> Easing.CUBIC_OUT;
            case "cubic_in_out" -> Easing.CUBIC_IN_OUT;
            case "quartic_in" -> Easing.QUARTIC_IN;
            case "quartic_out" -> Easing.QUARTIC_OUT;
            case "quartic_in_out" -> Easing.QUARTIC_IN_OUT;
            case "quintic_in" -> Easing.QUINTIC_IN;
            case "quintic_out" -> Easing.QUINTIC_OUT;
            case "quintic_in_out" -> Easing.QUINTIC_IN_OUT;
            case "sine_in" -> Easing.SINE_IN;
            case "sine_out" -> Easing.SINE_OUT;
            case "sine_in_out" -> Easing.SINE_IN_OUT;
            case "expo_in" -> Easing.EXPO_IN;
            case "expo_out" -> Easing.EXPO_OUT;
            case "expo_in_out" -> Easing.EXPO_IN_OUT;
            case "circ_in" -> Easing.CIRC_IN;
            case "circ_out" -> Easing.CIRC_OUT;
            case "circ_in_out" -> Easing.CIRC_IN_OUT;
            case "elastic_in" -> Easing.ELASTIC_IN;
            case "elastic_out" -> Easing.ELASTIC_OUT;
            case "elastic_in_out" -> Easing.ELASTIC_IN_OUT;
            case "back_in" -> Easing.BACK_IN;
            case "back_out" -> Easing.BACK_OUT;
            case "back_in_out" -> Easing.BACK_IN_OUT;
            default -> null; // or throw an exception if preferred
        };
    }
}
