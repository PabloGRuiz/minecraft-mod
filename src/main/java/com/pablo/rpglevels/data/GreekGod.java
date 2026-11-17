package com.pablo.rpglevels.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

public enum GreekGod {
    ZEUS(
            "zeus",
            "Zeus",
            "Rey del Olimpo y Dios del Trueno",
            "⚡",
            ChatFormatting.YELLOW,
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
    private final String introLine1;
    private final String introLine2;
    private final String introLine3;
    private final String ascensionCongratulation;

    GreekGod(String id, String displayName, String title, String icon, ChatFormatting color,
             String introLine1, String introLine2, String introLine3, String ascensionCongratulation) {
        this.id = id;
        this.displayName = displayName;
        this.title = title;
        this.icon = icon;
        this.color = color;
        this.introLine1 = introLine1;
        this.introLine2 = introLine2;
        this.introLine3 = introLine3;
        this.ascensionCongratulation = ascensionCongratulation;
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
