package com.pablo.rpglevels.social;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.items.AdventurerCardItem;
import com.pablo.rpglevels.items.ModItems;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;

import java.util.List;
import java.util.Random;

public class NpcSocialManager {

    public enum RelationshipTier {
        DESCONOCIDO("Desconocido", "§7", 0),
        CONOCIDO("Conocido", "§e", 25),
        AMISTOSO("Amistoso", "§a", 50),
        AMIGO_CERCANO("Amigo Cercano", "§b", 75),
        VINCULO_INQUEBRANTABLE("Vínculo Leal", "§d", 90);

        private final String displayName;
        private final String colorCode;
        private final int minPoints;

        RelationshipTier(String displayName, String colorCode, int minPoints) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.minPoints = minPoints;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColorCode() {
            return colorCode;
        }

        public static RelationshipTier fromPoints(int points) {
            if (points >= 90) return VINCULO_INQUEBRANTABLE;
            if (points >= 75) return AMIGO_CERCANO;
            if (points >= 50) return AMISTOSO;
            if (points >= 25) return CONOCIDO;
            return DESCONOCIDO;
        }
    }

    private static final Random RNG = new Random();

    public static record GiftResult(int affinityGain, String responseDialogue, boolean isLoved) {}

    public static GiftResult evaluateGift(ItemStack stack, boolean isAdventurer) {
        return evaluateGift(stack, 0, isAdventurer);
    }

    public static GiftResult evaluateGift(ItemStack stack, int personalityId, boolean isAdventurer) {
        if (stack.isEmpty()) {
            return new GiftResult(0, "No tienes ningún objeto preparado para regalar en tu mano.", false);
        }

        // 1. PROTECCIÓN ABSOLUTA: Prohibido regalar armas espirituales, artefactos únicos o equipamiento vital
        if (stack.getItem() instanceof SpiritualSwordItem
                || stack.is(ModItems.SPIRITUAL_SWORD.get())
                || stack.is(ModItems.CULTIST_SPIRITUAL_SWORD.get())
                || stack.is(ModItems.ADVENTURER_CARD.get())
                || stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem) {
            String warning = isAdventurer
                    ? "§c\"¡Espera un momento, camarada! Ese es tu equipamiento personal o un artefacto sagrado. No puedo aceptar algo tan valioso e insustituible, consérvalo para tus batallas.\""
                    : "§c\"¡Por los cielos, no puedo aceptar algo tan peligroso o valioso! Por favor consérvalo, a ti te será de mucha más utilidad.\"";
            return new GiftResult(0, warning, false);
        }

        // Categorización de Regalos
        boolean isCommonMetal = stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT);
        boolean isPreciousMetal = stack.is(Items.GOLD_INGOT) || stack.is(Items.NETHERITE_INGOT);
        boolean isGem = stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || stack.is(Items.AMETHYST_SHARD) || stack.is(Items.LAPIS_LAZULI);
        boolean isFlower = stack.is(ItemTags.FLOWERS);
        boolean isSweet = stack.is(Items.CAKE) || stack.is(Items.PUMPKIN_PIE) || stack.is(Items.APPLE)
                || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.HONEY_BOTTLE) || stack.is(Items.COOKIE);

        // Si el ítem no pertenece a ninguna categoría de regalo reconocida: RECHAZO SEGURO (0 afinidad, NO se consume)
        if (!isCommonMetal && !isPreciousMetal && !isGem && !isFlower && !isSweet) {
            String reject = isAdventurer
                    ? "§7\"Agradezco la intención, camarada, pero no puedo aceptar esto. No es algo que me sea de utilidad en mis labores de aventurero.\""
                    : "§7\"Muchas gracias, pero preferiría no quedarme con esto. Quédatelo tú, por favor.\"";
            return new GiftResult(0, reject, false);
        }

        // Personalidad dinámica (5 arquetipos de gustos)
        int personality = Math.abs(personalityId) % 5;
        switch (personality) {
            case 0 -> { // HERRERO TRADICIONAL (Ama metales comunes, odia gemas llamativas)
                if (isCommonMetal) {
                    return new GiftResult(18, isAdventurer
                            ? "§6\"¡Por la forja! ¡Un lingote de metal puro y bien templado! Esto mantendrá mi filo impecable en las expediciones, ¡mil gracias!\""
                            : "§6\"¡Qué buen metal para el taller! Justo lo que hacía falta para las reparaciones de la aldea. ¡Muchas gracias!\"", true);
                }
                if (isGem) {
                    return new GiftResult(0, "§c\"¿Piedras brillantes? No tienen filo ni temple para una buena espada. Guárdate eso, prefiero herramientas de verdad.\"", false);
                }
            }
            case 1 -> { // MERCADER AMBICIOSO (Ama gemas y oro, odia flores silvestres)
                if (isGem || isPreciousMetal) {
                    return new GiftResult(18, isAdventurer
                            ? "§6\"¡Santo cielo, qué brillo tan exquisito! ¡Esto vale una auténtica fortuna en el gremio! ¡Cuenta conmigo para lo que sea!\""
                            : "§6\"¡Qué joya tan radiante y valiosa! Es el regalo más generoso que he recibido, ¡muchísimas gracias!\"", true);
                }
                if (isFlower) {
                    return new GiftResult(0, "§c\"¿Hierbas silvestres? ¿Acaso parezco un jarrón? Si vas a hacerme un obsequio, que tenga valor duradero, camarada.\"", false);
                }
            }
            case 2 -> { // ALMA ROMÁNTICA / BOTÁNICO (Ama flores y dulces, odia hierro tosco)
                if (isFlower || isSweet) {
                    return new GiftResult(18, isAdventurer
                            ? "§d\"¡Qué detalle tan hermoso y fragante! Incluso en los caminos más oscuros, un gesto así alegra el corazón de un guerrero. ¡Gracias!\""
                            : "§d\"¡Qué hermosura de flores! Tienen un aroma delicioso y alegran todo el hogar. ¡Me encanta de corazón!\"", true);
                }
                if (isCommonMetal) {
                    return new GiftResult(0, "§c\"Ugh... ¿un trozo de metal tosco y frío? Bastantes asperezas tiene ya el mundo como para cargar con más chatarra.\"", false);
                }
            }
            case 3 -> { // CAZADOR GOURMET (Ama dulces y manzanas doradas, odia metales)
                if (isSweet) {
                    return new GiftResult(18, isAdventurer
                            ? "§e\"¡Ufff, qué delicia de bocado! El camino desgasta a cualquiera y un buen dulce reconforta más que mil palabras. ¡Genial!\""
                            : "§e\"¡Huele exquisito! Me fascina la buena repostería. ¡Qué detalle tan dulce y considerado!\"", true);
                }
                if (isCommonMetal) {
                    return new GiftResult(0, "§c\"¿Lingotes? ¡No puedo comerme un pedazo de hierro! Tráeme algo para el paladar la próxima vez.\"", false);
                }
            }
            default -> { // 4: ERUDITO MÍSTICO (Ama gemas arcanas, odia oro vano)
                if (isGem) {
                    return new GiftResult(18, isAdventurer
                            ? "§b\"Fascinante... Puedo percibir la resonancia mágica atrapada en esta gema. Es un espécimen magnífico para mis estudios arcanos.\""
                            : "§b\"¡Qué piedra tan enigmática y hermosa! Brilla con un aura muy especial. ¡La atesoraré!\"", true);
                }
                if (isPreciousMetal) {
                    return new GiftResult(0, "§c\"El oro solo atrae a los insensatos y ladrones. Prefiero minerales con auténtico potencial arcano.\"", false);
                }
            }
        }

        // Si es neutral pero de las categorías aceptadas:
        String neutralQuote = isAdventurer
                ? "§a\"Agradezco mucho el gesto, colega. Todo recurso ayuda a mantener los ánimos altos en la expedición.\""
                : "§a\"Muchas gracias por pensar en mí y traerme este detalle. ¡Es muy amable de tu parte!\"";
        return new GiftResult(6, neutralQuote, false);
    }

    public static GiftResult evaluateAliceGift(ItemStack stack, boolean hasDeliveredShadowblade) {
        if (stack.isEmpty()) {
            return new GiftResult(0, "No tienes ningún objeto preparado para regalar en tu mano.", false);
        }

        // Antes de entregar la Shadowblade: Alice solo acepta la Shadowblade del Cultista
        if (!hasDeliveredShadowblade) {
            if (stack.is(ModItems.CULTIST_SPIRITUAL_SWORD.get())) {
                return new GiftResult(50,
                        "§5«¡Por todos los espíritus ancestrales! ¿Es la hoja oscura del Cultista? ¡La has recuperado! Has purificado un gran mal y demostrado la nobleza de tu alma. A partir de hoy, tienes mi lealtad y mi espada a tu servicio. Cuando necesites de mis artes espirituales en el campo de batalla, solo pídeme que te acompañe.»",
                        true);
            }
            return new GiftResult(0,
                    "§d«Agradezco el gesto, pero mi mente está atormentada por la traición del Segador Corrupto... Hasta que no tengamos en nuestro poder su espada maldita, la Shadowblade del Cultista, no puedo distraerme con cortesías ni presentes.»",
                    false);
        }

        // Tras entregar la Shadowblade: Gustos específicos de Alice
        if (stack.is(Items.AMETHYST_SHARD) || stack.is(Items.AMETHYST_CLUSTER) || stack.is(Items.LAPIS_LAZULI) || stack.is(Items.ECHO_SHARD) || stack.is(ItemTags.FLOWERS)) {
            return new GiftResult(18,
                    "§d«¡Qué ofrenda tan rebosante de energía espiritual! Resuena fuertemente con las artes de los Segadores. ¡Muchísimas gracias, camarada!»",
                    true);
        }

        if (stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || stack.is(Items.GOLD_INGOT)) {
            return new GiftResult(8,
                    "§d«Un recurso valioso para financiar nuestras investigaciones arcanas. Te lo agradezco de corazón.»",
                    false);
        }

        return new GiftResult(0,
                "§d«No puedo aceptar esto, camarada. Consérvalo para tus propios preparativos de combate.»",
                false);
    }

    public static String getChatDialogue(String npcType, int affinity, AdventurerRank playerRank, String npcName, String playerName) {
        RelationshipTier tier = RelationshipTier.fromPoints(affinity);

        if ("adventurer".equals(npcType)) {
            return switch (tier) {
                case DESCONOCIDO -> {
                    String[] dialogues = {
                            "Saludos, forastero. Dicen que los Dioses observan desde el Plano Astral; sin su Bendición Divina, ningún mortal puede subir de nivel.",
                            "Veo que portas el rango " + playerRank.getName() + ". Recuerda que para progresar en los rangos debes llegar a los Hitos de Nivel para que el Dios cuya bendición posees te otorgue la Ascensión al dormir.",
                            "Cuando abatí a mi primer monstruo y dormí en la posada, desperté en el Espacio Astral y escuché la voz de mi Dios otorgándome su bendición.",
                            "¿Vienes a buscar contratos? Cumplir misiones del gremio, cazar monstruos y saquear cofres antiguos es la mejor forma de alcanzar tu próximo Hito.",
                            "A los Dioses no les importa si tus hazañas son moralmente correctas o despiadadas, solo buscan que fortalezcas tu existencia en su nombre.",
                            "Muchos parten buscando la gloria, pero solo quienes alcanzan el Hito de su rango son convocados de nuevo al Plano Astral para ascender."
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
                case CONOCIDO -> {
                    String[] dialogues = {
                            "¡Hola, colega! ¿Qué Dios Olímpico te otorgó su bendición en el Plano Astral? En el gremio hay elegidos de Zeus, Ares, Atenea y hasta del mismo Hades.",
                            "Siempre es bueno cruzar miradas con alguien conocido. Si necesitas un compañero para subir de nivel y acercarte a tu Hito de Rango, por una esmeralda te cubro las espaldas.",
                            "Dicen que al llegar al Hito de tu rango actual y dormir, vuelves al Espacio Astral y un rayo divino sella tu Ascensión.",
                            "He estado entrenando duro. Quiero llenar mi recipiente mortal y alcanzar el próximo Hito para que mi Dios me conceda el ascenso.",
                            "La recompensa de experiencia por cumplir encargos del gremio ayuda muchísimo a llegar al Hito de Ascensión.",
                            "¿Recuerdas el rayo que cae sobre ti cuando regresas del Plano Astral? Todavía se me eriza la piel al recordarlo."
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
                case AMISTOSO -> {
                    String[] dialogues = {
                            "¡Qué alegría verte de nuevo, " + playerName + "! Seguro que el Dios cuya bendición portas está orgulloso de tus hazañas.",
                            "Pelear a tu lado hace que subir de nivel hacia el próximo Hito sea mucho más emocionante. ¡Somos un gran equipo!",
                            "Me gusta tu forma de combatir: se nota que la Bendición Divina corre con fuerza por tus venas.",
                            "Cuando alcances el Hito de tu rango actual, no olvides ir a dormir a una cama para que tu Dios te otorgue la Ascensión en el Espacio Astral.",
                            "Si alguna vez necesitas que monte guardia en tu campamento mientras sueñas en el Plano Astral con tu Dios, solo dímelo.",
                            "En el gremio ya muchos comentan tus andanzas. ¡Dicen que ascenderás de rango más rápido que nadie!"
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
                case AMIGO_CERCANO -> {
                    String[] dialogues = {
                            "¡Amigo mío! Brindemos por nuestras victorias y por los Dioses que nos bendijeron en el Plano Astral.",
                            "He conocido muchos elegidos de los Dioses en el gremio, pero pocos con tu temple, honor y lealtad. Es un privilegio marchar a tu lado.",
                            "Cada vez que superas un Hito y regresas del Espacio Astral tras recibir la Ascensión de tu Dios, tu aura se siente mucho más imponente.",
                            "Toma, estuve revisando mis suministros y separé provisiones para nuestras expediciones rumbo a tu próximo Hito.",
                            "Antes pensaba que nuestras estadísticas de nacimiento decidían nuestro destino... pero con la Bendición Divina demostramos que no tenemos límites.",
                            "Pase lo que pase ahí fuera, que sepas que tienes mi espada incondicional. ¡Ninguna calamidad logrará quebrantar nuestra familia!",
                            "Tu liderazgo me inspira a fortalecer mi existencia cada día."
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
                case VINCULO_INQUEBRANTABLE -> {
                    String[] dialogues = {
                            "¡Hermano de armas! Mi espada, mi escudo y mi vida están consagradas a tu lado. ¡Ni los mismos monstruos del Abismo detendrán nuestro ascenso!",
                            "Tu presencia inspira a todo el Gremio. Los Dioses del Plano Astral seguramente contemplan tus hazañas con asombro.",
                            "Los plebeyos como yo solemos ser olvidados por la historia... pero junto a un elegido como tú estamos escribiendo una leyenda digna del Olimpo.",
                            "Hermano, en la batalla somos un solo cuerpo. Mi vida es tu escudo mientras avanzas de Hito en Hito hacia la cumbre.",
                            "Si los mismos Dioses del Plano Astral bajaran a ponernos a prueba, empuñaría mi arma a tu lado sin dudar un solo parpadeo.",
                            "Más que un compañero de expediciones, eres mi verdadera familia. No hay oro en todo el continente que pueda pagar lo que significa tu lealtad.",
                            "¡Mira este horizonte! Con la Bendición Divina y nuestra hermandad, nada ni nadie nos podrá detener jamás.",
                            "¡Por nuestro linaje, por nuestras hazañas y por la gloria eterna! ¡A la carga, camarada!"
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
            };
        } else if ("villager".equals(npcType)) {
            return switch (tier) {
                case DESCONOCIDO -> RNG.nextBoolean()
                        ? "Saludos, viajero. Por aquí la vida es tranquila, siempre que los zombis se mantengan a raya."
                        : "No suelo ver forasteros con tantas armas. Espero que tus intenciones en la aldea sean pacíficas.";
                case CONOCIDO -> RNG.nextBoolean()
                        ? "¡Hola de nuevo! Estaba descansando un poco antes de dar mi paseo diario por la plaza."
                        : "Se rumorea que en las cuevas cercanas se oyen ecos extraños. Menos mal que tenemos el golem cerca.";
                case AMISTOSO -> RNG.nextBoolean()
                        ? "¡Qué tal, " + playerName + "! Me alegra mucho verte. ¿Traes novedades de tus aventuras?"
                        : "Siempre es un placer tenerte en la aldea. Eres un buen amigo para todos nosotros.";
                case AMIGO_CERCANO -> RNG.nextBoolean()
                        ? "¡Amigo mío! Si alguna vez necesitas refugio o un buen tazón de sopa, sabes que mi puerta está abierta."
                        : "Desde que visitas la aldea, todos nos sentimos mucho más seguros y tranquilos. ¡Gracias por cuidarnos!";
                case VINCULO_INQUEBRANTABLE -> RNG.nextBoolean()
                        ? "¡" + playerName + ", el orgullo de nuestra aldea! ¡Nuestra amistad vale más que cualquier esmeralda del reino!"
                        : "¡Siempre podrás contar conmigo para lo que necesites, camarada! Eres una verdadera leyenda.";
            };
        } else {
            // Townswoman
            return switch (tier) {
                case DESCONOCIDO -> RNG.nextBoolean()
                        ? "Hola, forastero. Bienvenido a nuestra aldea, ojalá tu estancia sea tranquila y pacífica."
                        : "Ten cuidado si caminas por el bosque al anochecer, se escuchan rumores de forajidos cerca.";
                case CONOCIDO -> RNG.nextBoolean()
                        ? "¡Buen día! Hoy el viento trae buen aroma en la aldea. Espero que tu jornada de aventuras sea provechosa."
                        : "Me agrada ver que la aldea sigue en calma gracias a la presencia de aventureros como tú.";
                case AMISTOSO -> RNG.nextBoolean()
                        ? "¡Hola, " + playerName + "! Me alegra mucho verte. Estaba horneando y cuidando del pueblo, te ves lleno de vitalidad."
                        : "Siempre que pasas por aquí se siente una gran seguridad. Gracias por ser tan atento con todos nosotros.";
                case AMIGO_CERCANO -> RNG.nextBoolean()
                        ? "¡Qué felicidad tenerte de visita! Cada vez que regresas sano y salvo de tus viajes respiro con alivio. ¡No olvides cuidarte!"
                        : "Eres más que un protector para nosotros, eres un amigo entrañable. Siempre tendrás un hogar y una bienvenida cálida aquí.";
                case VINCULO_INQUEBRANTABLE -> RNG.nextBoolean()
                        ? "¡" + playerName + "! Mi corazón se llena de júbilo cada vez que te veo llegar. ¡El pueblo entero canta tus hazañas y yo siempre rezaré por tu bienestar!"
                        : "Siempre contarás con mi más sincera admiración y afecto. ¡Eres nuestro héroe legendario!";
            };
        }
    }

    public static String getGreeting(String npcType, int affinity, AdventurerRank playerRank, String npcName, String playerName, boolean isHired, boolean isStaying) {
        RelationshipTier tier = RelationshipTier.fromPoints(affinity);

        if ("adventurer".equals(npcType)) {
            if (isHired) {
                if (isStaying) {
                    return "§e\"Aquí esperaré montando guardia hasta que me ordenes avanzar, " + playerName + ".\"";
                } else {
                    return "§a\"¡Cubriendo tu retaguardia, " + playerName + "! Marca el rumbo y te seguiré de cerca.\"";
                }
            }

            return switch (tier) {
                case DESCONOCIDO -> RNG.nextBoolean()
                        ? "§f\"Saludos, portador del " + playerRank.getName() + ". ¿Vienes por contratos o provisiones?\""
                        : "§f\"Buen día, forastero. El gremio ofrece buenos encargos si sabes manejar tu espada.\"";
                case CONOCIDO -> RNG.nextBoolean()
                        ? "§f\"¡Hola, camarada " + playerName + "! ¿Listo para otra jornada en los caminos?\""
                        : "§f\"¡Ey, " + playerName + "! Qué buen momento para cruzarnos entre expediciones.\"";
                case AMISTOSO -> RNG.nextBoolean()
                        ? "§a\"¡Qué buen momento para encontrarnos, amigo! ¿Hacia dónde marchamos hoy?\""
                        : "§a\"¡" + playerName + "! Siempre es un gran alivio ver una cara amiga y confiable.\"";
                case AMIGO_CERCANO -> RNG.nextBoolean()
                        ? "§b\"¡" + playerName + ", colega querido! Siempre es un inmenso orgullo estrechar tu mano.\""
                        : "§b\"¡Amigo del alma! Estaba esperando verte. Las sendas son más brillantes cuando marchamos juntos.\"";
                case VINCULO_INQUEBRANTABLE -> RNG.nextBoolean()
                        ? "§d\"¡Mi respetado líder y hermano! Mi espada y mi lealtad eterna permanecen firmes a tu lado.\""
                        : "§d\"¡Hermano mío! Verte llegar renueva mis fuerzas y enciende el espíritu de nuestra hermandad.\"";
            };
        } else if ("villager".equals(npcType)) {
            return switch (tier) {
                case DESCONOCIDO -> "§f\"Hola. ¿Buscas trabajo o solo estás de paso por nuestra aldea?\"";
                case CONOCIDO -> "§f\"¡Hola, viajero! Qué buen día para recorrer el pueblo.\"";
                case AMISTOSO -> "§a\"¡Hombre, " + playerName + "! Qué gusto encontrarte por aquí.\"";
                case AMIGO_CERCANO -> "§b\"¡" + playerName + ", querido amigo! Qué alegría cruzarme contigo.\"";
                case VINCULO_INQUEBRANTABLE -> "§d\"¡Mi gran hermano " + playerName + "! ¡La aldea siempre te recibe con los brazos abiertos!\"";
            };
        } else {
            return switch (tier) {
                case DESCONOCIDO -> "§f\"Hola, noble viajero. ¿Puedo ayudarte en algo?\"";
                case CONOCIDO -> "§f\"¡Buenos días, aventurero! Qué agradable verte por nuestra aldea.\"";
                case AMISTOSO -> "§a\"¡Hola, " + playerName + "! Me alegra verte con tan buen semblante hoy.\"";
                case AMIGO_CERCANO -> "§b\"¡Qué alegría que hayas venido a visitarme, " + playerName + "! Siempre eres muy bienvenido.\"";
                case VINCULO_INQUEBRANTABLE -> "§d\"¡" + playerName + "! Verte llegar es la mayor alegría de mi día.\"";
            };
        }
    }

    public static void spawnAffinityEffects(LivingEntity entity, boolean loved) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            if (loved) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        entity.getX(), entity.getY() + 1.6, entity.getZ(),
                        8, 0.4, 0.3, 0.4, 0.1);
                serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8F, 1.6F);
            } else {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        entity.getX(), entity.getY() + 1.4, entity.getZ(),
                        6, 0.35, 0.3, 0.35, 0.05);
                serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.1F);
            }
        }
    }
}
