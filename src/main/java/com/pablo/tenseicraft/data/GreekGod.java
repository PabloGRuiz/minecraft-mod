package com.pablo.tenseicraft.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

import java.util.List;

public enum GreekGod {
    ZEUS(
            "zeus",
            "Zeus",
            "Rey del Olimpo y Dios del Trueno",
            "⚡",
            ChatFormatting.YELLOW,
            "Chispa del Olimpo",
            "Inmunidad parcial a rayos y probabilidad de desatar relámpagos en cadena.",
            "¡Alza la vista ante la tormenta, mortal! Soy Zeus, Soberano del Olimpo y Señor de los Cielos.",
            "Tu primer derramamiento de sangre resonó como un trueno hasta mi trono. Los mortales comunes tiemblan ante el destino, pero en ti veo la chispa de un conquistador.",
            "Te otorgo mi Bendición Divina. Fortalece tu existencia, domina este mundo y logra grandes hazañas en mi nombre, ¡sin importar si son justas o despiadadas! Cuando alcances cada Hito de poder, duerme y te concederé la Ascensión.",
            "¡El trueno ruge en tu honor! Has llevado mi bendición hasta el límite de tu recipiente actual y tus hazañas han sacudido la tierra."
    ),
    HADES(
            "hades",
            "Hades",
            "Señor del Inframundo y los Muertos",
            "💀",
            ChatFormatting.DARK_GRAY,
            "Cosecha de Almas",
            "Daño extra a no-muertos y recuperación de vida y maná al abatir enemigos.",
            "No temas a la oscuridad que te rodea, mortal... Soy Hades, Soberano del Inframundo y Guardián de las Almas.",
            "Cuando enviaste a esa primera criatura a mis dominios, percibí el frío temple de tu espíritu. La vida y la muerte son meras monedas en el tablero de los dioses.",
            "Grabo mi Bendición en tu alma. Fortalece tu existencia y cosecha grandes hazañas en mi nombre, ¡sin importar si el mundo las juzga nobles o crueles! Alcanza los Hitos de poder y vuelve a soñar para recibir tu Ascensión.",
            "Más almas han caído bajo tu paso y tu recipiente mortal ha alcanzado su plenitud. El Inframundo reconoce tu implacable ascenso."
    ),
    POSEIDON(
            "poseidon",
            "Poseidón",
            "Señor de los Mares y las Tempestades",
            "🔱",
            ChatFormatting.DARK_AQUA,
            "Furia de las Mareas",
            "Velocidad, respiración y regeneración en agua/lluvia + daño aumentado al estar mojado.",
            "¡Escucha el rugido del abismo, mortal! Soy Poseidón, Sacudidor de la Tierra y Soberano de los Océanos.",
            "Como una ola embravecida que reclama la costa, tu primer combate llamó mi atención desde las profundidades.",
            "Recibe mi Bendición Divina. Que tu fuerza crezca como un maremoto imparable y logres grandes hazañas en mi nombre, ¡sin importar qué leyes mortales debas arrasar! Supera cada Hito y en tus sueños te otorgaré la Ascensión.",
            "¡Imponente como la marea alta! Has desbordado los límites de tu rango actual con hazañas dignas de mi nombre."
    ),
    ARES(
            "ares",
            "Ares",
            "Dios de la Guerra y la Matanza",
            "⚔",
            ChatFormatting.DARK_RED,
            "Fervor de Batalla",
            "Ganas más daño cuanta menos vida tengas y robo de vida cuerpo a cuerpo bajo presión.",
            "¡Siente el fervor del acero, mortal! Soy Ares, Dios de la Guerra, la Sangre y el Conflicto Eterno.",
            "El aroma de tu primera victoria llegó hasta mí. En un mundo lleno de débiles, tu instinto de supervivencia brilla como una hoja recién forjada.",
            "Te concedo mi Bendición Divina. Fortalece tu existencia, aplasta a tus enemigos y logra grandes hazañas en mi nombre, ¡sin importar si son moralmente correctas o bañadas en caos! Llega a cada Hito y duerme para reclamar tu Ascensión.",
            "¡Glorioso! La sangre derramada en tus batallas ha templado tu cuerpo hasta el límite de este rango. ¡Ruge con más fuerza, mi campeón!"
    ),
    ATHENA(
            "athena",
            "Atenea",
            "Diosa de la Sabiduría y la Estrategia",
            "🦉",
            ChatFormatting.AQUA,
            "Mente Estratégica",
            "Más XP RPG en todo momento y Parrys perfectos más frecuentes que restauran maná.",
            "Calma tu mente en este plano astral, mortal. Soy Atenea, Diosa de la Sabiduría, el Intelecto y la Guerra Táctica.",
            "Observé cómo abatiste a tu primer adversario. Incluso en la rudeza de un principiante, vi el germen de alguien capaz de alterar el curso de este mundo.",
            "Te otorgo mi Bendición Divina. Perfecciona tu existencia y ejecuta grandes hazañas en mi nombre, ¡más allá de la moralidad de los hombres! Cuando alcances el Hito de cada rango, descansa y actualizaré tu rango.",
            "Tu dominio y determinación han superado el umbral de tu rango actual. Has demostrado que mi inversión en ti fue acertada."
    ),
    APOLLO(
            "apollo",
            "Apolo",
            "Dios del Sol, la Luz y la Profecía",
            "☀",
            ChatFormatting.GOLD,
            "Gracia Solar",
            "Regeneración bajo la luz solar, velocidad de ataque y flechas imbuidas en fuego.",
            "¡Contempla el resplandor del cosmos, mortal! Soy Apolo, Señor del Sol Radiante, el Arco y el Oráculo.",
            "Mis ojos todo lo ven bajo el firmamento, y el destello de tu primera batalla brilló con una intensidad imposible de ignorar.",
            "Derramo mi Bendición sobre ti. Fortalece tu existencia y escribe una epopeya de grandes hazañas en mi nombre, ¡sin importar si traes luz o fuego abrasador! Alcanza cada Hito y vuelve a este sueño para ascender.",
            "¡Brillante como el sol del mediodía! Tus hazañas han eclipsado los límites de tu rango actual."
    ),
    ARTEMIS(
            "artemis",
            "Artemisa",
            "Diosa de la Caza y la Luna Salvaje",
            "🏹",
            ChatFormatting.GREEN,
            "Instinto de Cazadora",
            "Mayor probabilidad de crítico, visión nocturna y ventaja mortal a distancia o de noche.",
            "Has entrado en mis dominios estelares, mortal. Soy Artemisa, Señora de la Caza, la Naturaleza Indómita y la Luna.",
            "En este mundo tú eras la presa, pero al abatir a tu primer enemigo te convertiste en cazador. Ese instante capturó mi interés.",
            "Te marco con mi Bendición Divina. Fortalece tu existencia, da caza a lo imposible y logra grandes hazañas en mi nombre, ¡sin ataduras morales! Cuando alcances el Hito de tu rango, duerme y te otorgaré la Ascensión.",
            "Tu instinto de cazador se ha afilado hasta el límite de este rango. Ninguna presa podrá escapar de tu alcance."
    ),
    HERMES(
            "hermes",
            "Hermes",
            "Dios de los Viajeros, la Astucia y la Fortuna",
            "🪽",
            ChatFormatting.WHITE,
            "Bendición del Mensajero",
            "Más velocidad, Suerte aumentada, menor enfriamiento en Dash y botín extra en cofres.",
            "¡Vaya, vaya, qué sorpresa tenemos aquí! Soy Hermes, Mensajero de los Dioses, Señor de los Caminos y la Fortuna.",
            "De todos los mortales que vagan por estas tierras, tu primer paso en el combate tuvo ese toque de audacia que tanto me divierte.",
            "¡Toma mi Bendición Divina! Fortalece tu existencia, recorre el mundo y haz grandes hazañas en mi nombre, ¡sean heroicas, pícaras o moralmente cuestionables! Al llegar a cada Hito, echa una siesta y te ascenderé.",
            "¡Qué velocidad, qué estilo! Ya has llenado el recipiente de tu rango actual con hazañas dignas de una leyenda."
    ),
    HEPHAESTUS(
            "hephaestus",
            "Hefesto",
            "Dios de la Forja y el Fuego Volcánico",
            "🔨",
            ChatFormatting.RED,
            "Llama de la Forja",
            "Resistencia al fuego, armadura divina, autorreparación y fundición instantánea al minar.",
            "Escucha el martilleo del cosmos, mortal. Soy Hefesto, Señor de la Forja Divina y la Llama Inextinguible.",
            "Todo mortal nace como hierro en bruto, pero al enfrentar y vencer a tu primer enemigo, vi en ti el metal capaz de convertirse en una obra maestra.",
            "Grabo mi Bendición a fuego en tu ser. Forja y fortalece tu existencia, logrando grandes hazañas en mi nombre sin importar el juicio moral de los débiles. En cada Hito, duerme y templaré tu alma al siguiente rango.",
            "El fuego de mil combates ha endurecido tu espíritu hasta el tope de este rango. Es hora de forjar un nivel superior."
    ),
    APHRODITE(
            "aphrodite",
            "Afrodita",
            "Diosa de la Pasión y la Belleza Inmortal",
            "🌹",
            ChatFormatting.LIGHT_PURPLE,
            "Aura de Encanto",
            "Afinidad acelerada con aventureros, curación pasiva para aliados y defensa ante humanoides.",
            "Bienvenido a mi sueño astral, querido mortal. Soy Afrodita, Diosa de la Pasión, el Deseo y la Belleza Absoluta.",
            "No hay nada más cautivador que un mortal luchando con desesperación y fiereza por su vida. Tu primera victoria me resultó fascinante.",
            "Te concedo mi Bendición Divina. Embellece y fortalece tu existencia, desatando grandes hazañas en mi nombre, ¡sin importar si son dulces o despiadadas! Alcanza cada Hito y duerme para que eleve tu rango.",
            "¡Sencillamente deslumbrante! Tus hazañas han hecho florecer todo el potencial de tu rango actual."
    ),
    DIONYSUS(
            "dionysus",
            "Dioniso",
            "Dios del Éxtasis, el Vino y el Frenesí",
            "🍇",
            ChatFormatting.DARK_PURPLE,
            "Éxtasis del Banquete",
            "Frenesí combativo al comer o beber pociones y duración extendida en tus brebajes.",
            "¡Brindemos en el vacío estelar, mortal! Soy Dioniso, Dios del Éxtasis, el Banquete y la Locura Divina.",
            "El mundo mortal suele ser tan monótono, pero cuando abatiste a esa primera bestia sentí el embriagador aroma de una gran historia por comenzar.",
            "Te doy mi Bendición Divina. Fortalece tu existencia y desata grandes hazañas en mi nombre, ¡sin importar lo que la moral considere cuerdo o correcto! Cuando llegues a cada Hito, duerme y celebraremos tu Ascensión.",
            "¡Ja, ja, espléndido espectáculo! Has embriagado al mundo con tus hazañas y alcanzado el límite de tu rango."
    ),
    DEMETER(
            "demeter",
            "Deméter",
            "Diosa de la Tierra Sagrada y los Ciclos",
            "🌾",
            ChatFormatting.DARK_GREEN,
            "Ciclo de la Abundancia",
            "Crecimiento acelerado de cultivos, regeneración sobre tierra/pasto, cosecha extra y comidas nutritivas.",
            "Siente el pulso de la creación en este vacío, mortal. Soy Deméter, Señora de la Tierra Fértil, las Estaciones y la Vida.",
            "Para que una semilla crezca, primero debe romper la cáscara en la oscuridad. Al vencer a tu primer enemigo, tu verdadera existencia germinó.",
            "Te otorgo mi Bendición Divina. Nutre y fortalece tu existencia, logrando grandes hazañas en mi nombre sin importar si traes abundancia o invierno implacable. Alcanza cada Hito y al dormir te concederé la Ascensión.",
            "Tu poder ha madurado como una cosecha dorada hasta colmar tu rango actual. Es momento de expandir tus raíces."
    );

    private final String id;
    private final String displayName;
    private final String title;
    private final String icon;
    private final ChatFormatting color;
    private final String blessingName;
    private final String blessingShortSummary;
    private final String introLine1;
    private final String introLine2;
    private final String introLine3;
    private final String ascensionCongratulation;

    GreekGod(String id, String displayName, String title, String icon, ChatFormatting color,
             String blessingName, String blessingShortSummary,
             String introLine1, String introLine2, String introLine3, String ascensionCongratulation) {
        this.id = id;
        this.displayName = displayName;
        this.title = title;
        this.icon = icon;
        this.color = color;
        this.blessingName = blessingName;
        this.blessingShortSummary = blessingShortSummary;
        this.introLine1 = introLine1;
        this.introLine2 = introLine2;
        this.introLine3 = introLine3;
        this.ascensionCongratulation = ascensionCongratulation;
    }

    public String getBlessingName() {
        return blessingName;
    }

    public String getBlessingShortSummary() {
        return blessingShortSummary;
    }

    public static int getBlessingTier(AdventurerRank rank) {
        if (rank == null) return 1;
        if (rank.getIndex() >= AdventurerRank.B.getIndex()) return 3;
        if (rank.getIndex() >= AdventurerRank.D.getIndex()) return 2;
        return 1;
    }

    public List<String> getBlessingDetails(int tier) {
        int t = Math.max(1, Math.min(3, tier));
        return switch (this) {
            case ZEUS -> {
                int lightningRes = t == 1 ? 60 : (t == 2 ? 80 : 100);
                int stormDmg = t == 1 ? 8 : (t == 2 ? 12 : 16);
                int chains = t + 1;
                int chainPct = t == 1 ? 25 : (t == 2 ? 35 : 45);
                int cd = 7 - t;
                yield List.of(
                        "• Mitiga -" + lightningRes + "% daño de rayos y +" + stormDmg + "% de daño bajo lluvia o tormenta.",
                        "• Al atacar: 25% (50% bajo lluvia) de lanzar Relámpago en Cadena a " + chains + " enemigos.",
                        "• Cada descarga inflige " + chainPct + "% del golpe (Enfriamiento: " + cd + "s)."
                );
            }
            case HADES -> {
                int undeadDmg = t == 1 ? 12 : (t == 2 ? 18 : 25);
                int healPct = t == 1 ? 6 : (t == 2 ? 8 : 10);
                int manaGain = t == 1 ? 5 : (t == 2 ? 8 : 12);
                yield List.of(
                        "• Infliges +" + undeadDmg + "% de daño extra contra criaturas no-muertas.",
                        "• Cosecha al matar hostiles: recuperas " + healPct + "% de Vida Máx. (mín. 2 HP) y +" + manaGain + " PM."
                );
            }
            case POSEIDON -> {
                int wetDmg = t == 1 ? 12 : (t == 2 ? 18 : 25);
                int tridentDmg = t == 1 ? 20 : (t == 2 ? 30 : 40);
                yield List.of(
                        "• En agua o lluvia: Respiración Acuática, Gracia de Delfín y Regeneración I.",
                        "• Estando mojado: +" + wetDmg + "% de daño general (+" + tridentDmg + "% con Tridente)."
                );
            }
            case ARES -> {
                int maxBonus = t == 1 ? 20 : (t == 2 ? 30 : 40);
                int lifesteal = t == 1 ? 4 : (t == 2 ? 6 : 8);
                yield List.of(
                        "• Fervor: ganas hasta +" + maxBonus + "% de daño conforme disminuye tu salud.",
                        "• Bajo el 60% de HP, tus golpes cuerpo a cuerpo roban " + lifesteal + "% del daño como curación."
                );
            }
            case ATHENA -> {
                int xpPct = t == 1 ? 15 : (t == 2 ? 20 : 25);
                int parryBonus = t == 1 ? 10 : (t == 2 ? 15 : 20);
                int parryMana = t == 1 ? 10 : (t == 2 ? 15 : 20);
                yield List.of(
                        "• Sabiduría Marcial: +" + xpPct + "% XP RPG adicional en todas tus fuentes.",
                        "• +" + parryBonus + "% prob. de Parry Perfecto; al lograrlo recuperas +" + parryMana + " PM y obtienes Resistencia I (4s)."
                );
            }
            case APOLLO -> {
                String regen = t == 1 ? "0.5" : (t == 2 ? "1.0" : "1.5");
                int atkSpd = t == 1 ? 5 : (t == 2 ? 8 : 12);
                int projDmg = t == 1 ? 15 : (t == 2 ? 22 : 30);
                int burnSec = t + 2;
                yield List.of(
                        "• Bajo la luz solar: regeneras " + regen + " HP cada 2s y +" + atkSpd + "% Vel. de Ataque.",
                        "• Flechas y proyectiles infligen +" + projDmg + "% de daño e incendian no-muertos o de día (" + burnSec + "s)."
                );
            }
            case ARTEMIS -> {
                int nightSpd = t == 1 ? 8 : (t == 2 ? 12 : 16);
                int rangedDmg = t == 1 ? 15 : (t == 2 ? 22 : 30);
                int critBonus = t == 1 ? 8 : (t == 2 ? 12 : 15);
                yield List.of(
                        "• De noche: Visión Nocturna automática y +" + nightSpd + "% Velocidad de Movimiento.",
                        "• +" + rangedDmg + "% daño con arco/ballesta o a >8 bloques y +" + critBonus + "% Probabilidad de Crítico."
                );
            }
            case HERMES -> {
                int moveSpd = t == 1 ? 10 : (t == 2 ? 15 : 20);
                String luck = t == 1 ? "1.5" : (t == 2 ? "2.5" : "3.5");
                int dashRed = t == 1 ? 20 : (t == 2 ? 30 : 35);
                yield List.of(
                        "• Pasos Alados: +" + moveSpd + "% Velocidad, +" + luck + " Suerte y -" + dashRed + "% enfriamiento del Dash.",
                        "• Saquear cofres naturales otorga +50% XP RPG extra y Velocidad I durante 10s."
                );
            }
            case HEPHAESTUS -> {
                int fireRes = t == 1 ? 40 : (t == 2 ? 65 : 90);
                int armor = t * 2;
                int smeltPct = t == 1 ? 25 : (t == 2 ? 40 : 60);
                yield List.of(
                        "• Mitiga -" + fireRes + "% daño de fuego/lava, +" + armor + " Armadura y repara 1 durabilidad cada 3s.",
                        "• Al minar hierro, oro o cobre: " + smeltPct + "% de probabilidad de obtener 1 lingote fundido extra."
                );
            }
            case APHRODITE -> {
                int dmgRed = t == 1 ? 10 : (t == 2 ? 15 : 20);
                String heal = t == 1 ? "1.0" : (t == 2 ? "1.5" : "2.5");
                yield List.of(
                        "• +50% afinidad ganada con NPCs aventureros y -" + dmgRed + "% daño de bandidos y saqueadores.",
                        "• Cada 3s sanas " + heal + " HP a aliados, compañeros y guardián cercanos en 10 bloques."
                );
            }
            case DIONYSUS -> {
                int durSec = t == 1 ? 8 : (t == 2 ? 12 : 16);
                int mana = t == 1 ? 4 : (t == 2 ? 6 : 10);
                int extPct = t == 1 ? 25 : (t == 2 ? 35 : 50);
                yield List.of(
                        "• Al comer o beber pociones: Frenesí (" + durSec + "s Velocidad y Fuerza I) + " + mana + " PM.",
                        "• Tus pociones beneficiosas extienden automáticamente su duración en +" + extPct + "%."
                );
            }
            case DEMETER -> {
                int rad = t + 3;
                String regen = t == 1 ? "0.5" : (t == 2 ? "1.0" : "1.5");
                int extraHp = t == 1 ? 2 : (t == 2 ? 3 : 5);
                int cropPct = t == 1 ? 30 : (t == 2 ? 45 : 60);
                yield List.of(
                        "• Acelera cultivos en " + rad + " bloques cada 2s (incl. Flor de la Vida) y regeneras " + regen + " HP sobre tierra/pasto.",
                        "• Las comidas curan +" + extraHp + " HP extra y tienes " + cropPct + "% de cosecha adicional en cultivos maduros."
                );
            }
        };
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getIntroLine1() {
        return introLine1;
    }

    public String getIntroLine2() {
        return introLine2;
    }

    public String getIntroLine3() {
        return introLine3;
    }

    public String getAscensionCongratulation() {
        return ascensionCongratulation;
    }

    public MutableComponent getChatPrefix() {
        return Component.literal("[" + icon + " " + displayName + " - " + title + "] ")
                .withStyle(color, ChatFormatting.BOLD);
    }

    public MutableComponent getBadge() {
        return Component.literal(icon + " " + displayName).withStyle(color, ChatFormatting.BOLD);
    }

    public static GreekGod fromId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (GreekGod god : values()) {
            if (god.id.equalsIgnoreCase(id) || god.name().equalsIgnoreCase(id) || god.displayName.equalsIgnoreCase(id)) {
                return god;
            }
        }
        return null;
    }

    public static GreekGod pickRandom(RandomSource random) {
        GreekGod[] all = values();
        return all[random.nextInt(all.length)];
    }
}
