package com.application.canopy.model;

import java.util.List;

public class Plant {

        private final String id; // univoco, es: "lavanda"
        private final String name; // nome comune: "Lavanda"
        private final String curiosity; // es: "Lavandula angustifolia"
        private final String description; // descrizione in erbario
        private final String careTips; // "come prendersene cura"
        private final String folderName; // cartella immagini: "Lavanda"
        private final String thumbFile; // thumb: "Lavanda.png"
        private final String color; // hex color: "#FFB7C5"

        public Plant(String id,
                        String name,
                        String curiosity,
                        String description,
                        String careTips,
                        String folderName,
                        String thumbFile,
                        String color) {
                this.id = id;
                this.name = name;
                this.curiosity = curiosity;
                this.description = description;
                this.careTips = careTips;
                this.folderName = folderName;
                this.thumbFile = thumbFile;
                this.color = color;
        }

        public String getId() {
                return id;
        }

        public String getName() {
                return name;
        }

        public String getCuriosity() {
                return curiosity;
        }

        public String getDescription() {
                return description;
        }

        public String getCareTips() {
                return careTips;
        }

        public String getFolderName() {
                return folderName;
        }

        public String getThumbFile() {
                return thumbFile;
        }

        public String getColor() {
                return color;
        }

        // Lista delle piante
        public static List<Plant> samplePlants() {
                return List.of(
                                new Plant(
                                                "sakura",
                                                "Sakura",
                                                "In Giappone il fiore di sakura rappresenta la fugacità della vita. Ogni primavera le persone celebrano l'hanami, la tradizione di ammirare i ciliegi in fiore nei parchi e nei templi.",
                                                "Il ciliegio giapponese, simbolo di rinascita e bellezza effimera, è famoso per la sua spettacolare fioritura primaverile. I petali rosa pallido ricoprono i rami creando un paesaggio poetico, spesso associato al concetto giapponese di 'mono no aware', la malinconia delle cose che svaniscono. Cresce lentamente ma regala fioriture straordinarie quando ben curato.",
                                                "Necessita di piena luce solare e terreno ben drenato. Ama gli inverni freddi e le primavere miti. Durante il periodo vegetativo, annaffiare regolarmente evitando ristagni. Concimare due volte l?anno, preferendo fertilizzanti ricchi di fosforo per favorire la fioritura. Potare leggermente dopo la fioritura per mantenere la forma.",
                                                "Sakura",
                                                "Sakura.png",
                                                "#FFB7C5"),
                                new Plant(
                                                "quercia",
                                                "Quercia",
                                                "In molte culture la quercia è considerata sacra. I Celti la veneravano come albero del sapere, mentre nella mitologia nordica era associata al dio Thor.",
                                                "Simbolo di forza e longevità, la quercia è uno degli alberi più maestosi delle foreste temperate. Le sue radici profonde e il tronco robusto ne fanno una presenza imponente, mentre le foglie lobate cambiano colore con le stagioni. Produce ghiande che nutrono la fauna selvatica e contribuiscono all'equilibrio dell'ecosistema.",
                                                "Predilige pieno sole e terreno profondo, leggermente acido e ben drenato. Nelle prime fasi di crescita richiede annaffiature regolari, poi diventa autonoma. Non teme il freddo e può vivere secoli. La potatura va fatta in inverno, eliminando i rami secchi o malformati.",
                                                "Quercia",
                                                "Quercia.png",
                                                "#8B4513"),
                                new Plant(
                                                "menta",
                                                "Menta",
                                                "Nella mitologia greca, Menta era una ninfa trasformata in pianta da Persefone. Da allora è simbolo di rinascita e freschezza.",
                                                "Pianta aromatica perenne conosciuta per il suo profumo fresco e le proprietà digestive. Le sue foglie verdi contengono oli essenziali di mentolo che donano un aroma intenso e rinfrescante. È ideale in tisane, dolci, piatti salati e anche come repellente naturale per insetsi.",
                                                "Ama la luce ma non il sole diretto nelle ore più calde. Va coltivata in terreno fresco e umido, con annaffiature regolari e abbondanti durante l?estate. Cresce rapidamente: è consigliabile tenerla in vaso per controllarne l?espansione. Taglia spesso le cime per stimolare nuove foglie.",
                                                "Menta",
                                                "Menta.png",
                                                "#98FF98"),
                                new Plant(
                                                "lavanda",
                                                "Lavanda",
                                                "I Romani usavano la lavanda nei bagni termali per le sue proprietà purificanti. Il nome deriva dal latino 'lavare'.",
                                                "Arbusto perenne tipico del Mediterraneo, celebre per i suoi fiori violacei e il profumo rilassante. Le spighe fiorite sono ricche di oli essenziali usati in aromaterapia, cosmetica e profumeria. Il suo portamento ordinato e i colori delicati la rendono perfetta per bordure o vasi soleggiati.",
                                                "Predilige il pieno sole e terreni aridi, calcarei e ben drenati. Annaffiare solo quando il terreno è completamente asciutto. Potare ogni anno dopo la fioritura per mantenere la forma compatta e stimolare nuovi germogli.",
                                                "Lavanda",
                                                "Lavanda.png",
                                                "#E6E6FA"),
                                new Plant(
                                                "peperoncino",
                                                "Peperoncino",
                                                "Nell’antico Perù il peperoncino era usato come mezzo di scambio. Le bacche senza picciolo e liberate dai semi, erano chiamate “guaine” e venivano usate nei mercati come moneta. Fino alla metà del XX secolo, nella piazza del mercato di Cuzco si potevano comprare merci con una manciata di peperoncini (in genere una mezza dozzina) detta rantii.",
                                                "il frutto di alcune varietà piccanti di piante del genere Capsicum, della famiglia delle Solanaceae, originarie del Centro e Sud America.",
                                                "E' fondamentale fornirle molta luce solare diretta, annaffiarla regolarmente senza esagerare (evitando ristagni d'acqua e bagnando solo il terreno), e concimare con un prodotto ricco di potassio durante la fase di crescita e fruttificazione",
                                                "Peperoncino",
                                                "Peperoncino.png",
                                                "#FF0000"),
                                new Plant(
                                                "orchidea",
                                                "Orchidea",
                                                "Le orchidee rappresentano amore e raffinatezza. Alcune specie vivono decenni e sono impollinate solo da insetti specifici.",
                                                "Pianta elegante e raffinata, apprezzata per i fiori di lunga durata e la straordinaria varietà di forme e colori. Le radici aeree assorbono umidità e sostanze nutritive dall?aria, rendendola perfetta per interni luminosi e ambienti umidi.",
                                                "Richiede luce diffusa e costante umidità ambientale. Innaffiare una volta a settimana, preferendo immersioni brevi del vaso. Evitare ristagni e spruzzare acqua sulle radici aeree. Concimare ogni due settimane con fertilizzante bilanciato. Dopo la fioritura, tagliare lo stelo sopra il nodo per favorire nuovi germogli.",
                                                "Orchidea",
                                                "Orchidea.png",
                                                "#DA70D6"),
                                new Plant(
                                                "lifeblood",
                                                "Lifeblood",
                                                "La Creatura di Grimm e le piccole tessitrici evocate dal Canto della Tessitrice non posso assimilare i Semi Vitali.",
                                                "Piccolo seme irrequieto pieno di sangue vitale, che può essere estratto e consumato per le sue proprietà curative.",
                                                "Cresce molto solo se circondata da simili, i suoi frutti potrebbero correre via ma non preoccuparti.",
                                                "Lifeblood",
                                                "Lifeblood.png",
                                                "#00BFFF"),
                                new Plant(
                                                "radice_sussurrante",
                                                "Radice Sussurrante",
                                                "Secondo le leggende di Nidosacro, chi dorme sotto il suo tronco può rivivere i ricordi perduti e trovare risposte alle domande dimenticate.",
                                                "Le Radici Sussurranti sono delle piante ripiene di Essenze che si trovano nel lungo e il largo di Nidosacro. Quando si colpisce una radice con l'Aculeo dei Sogni, questa farà comparire dei globi rossi che raccontano le storie di ha vissuto in quei luoghi.",
                                                "Si nutre dei ricordi di chi ci vive vicino. Non piantarlo troppo vicino pensieri pesanti.",
                                                "Radice_Sussurrante",
                                                "Radici_sussurranti.png",
                                                "#C71585"));
        }
}
