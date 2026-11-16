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
                            "Saludos, forastero. El gremio siempre necesita espadas afiladas, pero ten cuidado con lo que acecha tras la puesta de sol.",
                            "Veo que portas el rango " + playerRank.getName() + ". No bajes la guardia, los bosques están plagados de monstruos y ojos que vigilan.",
                            "Un consejo de colega: revisa bien el filo de tu arma y las costuras de tu armadura antes de adentrarte en cualquier gruta.",
                            "¿Vienes a buscar contratos? Las tabernas del reino bullen de rumores sobre ruinas olvidadas y bestias que custodian reliquias.",
                            "El camino de la aventura no perdona a los descuidados. Guarda siempre una antorcha y raciones en tu faltriquera.",
                            "Muchos parten buscando oro y gloria, pero pocos regresan para contarlo. Asegúrate de tener un buen plan de escape."
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
                case CONOCIDO -> {
                    String[] dialogues = {
                            "¡Hola, colega! Dicen que en las cavernas profundas se han avistado vetas resplandecientes y criaturas colosales.",
                            "Siempre es bueno cruzar miradas con alguien conocido. Si necesitas un compañero de expedición, por una esmeralda al día te cubro las espaldas.",
                            "Anoche escuché aullar lobos cerca de la colina. Si sales de expedición hoy, vigila tu retaguardia.",
                            "He estado entrenando mis estocadas y bloqueos con el escudo. En este oficio, dormirse en los laureles es firmar la sentencia de muerte.",
                            "La recompensa por cazar bandidos ha subido en el tablón del gremio. Esos rufianes están causando demasiados estragos en los caminos.",
                            "¿Has probado la sopa caliente de la posada? Tras diez horas marchando bajo la lluvia, sabe a auténtico banquete real."
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
                case AMISTOSO -> {
                    String[] dialogues = {
                            "¡Qué alegría verte de nuevo, " + playerName + "! Estaba afilando mi arma. ¿Qué hazaña tienes planeada para hoy?",
                            "Pelear a tu lado me hace sentir que podemos conquistar cualquier mazmorra. ¡Somos un gran equipo!",
                            "Me gusta tu forma de combatir: firme, valiente y con la cabeza fría. Es un alivio no tener que cubrir a un novato torpe.",
                            "¿Recuerdas esa última refriega? Por un momento creí que nos superaban en número, pero tu arrojo inclinó la balanza a nuestro favor.",
                            "Si alguna vez necesitas que monte guardia en tu campamento mientras descansas junto al fuego, solo dímelo. No se me escapará ni una sombra.",
                            "En el gremio ya muchos comentan nuestras andanzas. ¡Dicen que donde ponemos el pie, los monstruos huyen despavoridos!"
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
                case AMIGO_CERCANO -> {
                    String[] dialogues = {
                            "¡Amigo mío! Brindemos por nuestras victorias. Si alguna vez caes en una emboscada, no dudaré en ponerme al frente con mi escudo.",
                            "He conocido muchos guerreros en mis viajes, pero pocos con tu temple, honor y lealtad. Es un inmenso privilegio marchar a tu lado.",
                            "Cuando acabe nuestra jornada, deberíamos sentarnos junto a una fogata a compartir historias de nuestras tierras de origen. Considero un tesoro nuestra amistad.",
                            "Toma, estuve revisando mis suministros y separé las mejores vendas y provisiones por si las cosas se ponen difíciles en combate.",
                            "Antes luchaba únicamente por unas monedas de esmeralda... pero viajar contigo me recordó el verdadero espíritu de ser un aventurero.",
                            "Pase lo que pase ahí fuera, que sepas que tienes mi espada incondicional. ¡Ninguna calamidad logrará quebrantar nuestro equipo!",
                            "Tu liderazgo me inspira a ser mejor guerrero cada día. Siento que ya no soy el mismo aventurero inseguro que conoció las primeras ruinas."
                    };
                    yield dialogues[RNG.nextInt(dialogues.length)];
                }
                case VINCULO_INQUEBRANTABLE -> {
                    String[] dialogues = {
                            "¡Hermano de armas! Mi espada, mi escudo y mi vida están consagradas a tu lado. ¡Ningún monstruo ni bandido se interpondrá en nuestro camino!",
                            "Tu presencia inspira a todos los aventureros del reino. A donde tus pasos guíen, allí marcharé contigo hasta el fin de los mundos.",
                            "Los plebeyos como yo solemos ser olvidados por la historia... pero a tu lado siento que estamos escribiendo una leyenda imborrable.",
                            "Hermano, en la batalla somos un solo cuerpo. Mi vida es tu escudo, y mientras me queden fuerzas, ningún daño alcanzará tu pecho.",
                            "Si los mismos dioses del combate bajaran a desafiarnos, empuñaría mi arma a tu lado sin dudar un solo parpadeo.",
                            "Más que un compañero de expediciones, eres mi verdadera familia. No hay oro en todo el continente que pueda pagar lo que significa tu lealtad.",
                            "¡Mira este horizonte! El reino es vasto y peligroso, pero teniéndote a mi lado, siento que nada ni nadie nos podrá detener jamás.",
                            "¡Por nuestro linaje, por nuestras hazañas y por la gloria eterna de nuestra hermandad! ¡A la carga, camarada!"
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
