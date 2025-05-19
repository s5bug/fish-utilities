package tf.bug.fishutils.xivapi;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.*;
import discord4j.core.object.emoji.CustomEmoji;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Image;
import gg.xp.xivapi.XivApiClient;
import gg.xp.xivapi.assets.ImageFormat;
import gg.xp.xivapi.filters.SearchFilter;
import gg.xp.xivapi.filters.SearchFilters;
import gg.xp.xivapi.pagination.ListOptions;
import gg.xp.xivapi.pagination.XivApiPaginator;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;
import tf.bug.fishutils.Command;
import tf.bug.fishutils.FishUtilities;
import tf.bug.fishutils.xivapi.model.*;

public class XivActionCommand extends Command {
    public static final String ID = "xivaction";

    /// The spreadsheet row in `ClassJobCategory` corresponding to end-game jobs
    public static final int JOBS_CATEGORY_ROW_ID = 85;

    private final XivApiClient client;
    private final List<String> knownVersions;
    private final SortedSet<String> knownJobs;
    private final Map<String, ClassJob> allJobs;

    private final Map<String, CustomEmoji> emojiCache;
    private CustomEmoji rangeEmoji;
    private final Map<CastType, CustomEmoji> radiusEmoji;

    public XivActionCommand(XivApiClient client) {
        this.client = client;
        this.knownJobs = new TreeSet<>();
        this.allJobs = new HashMap<>();
        this.emojiCache = new HashMap<>();
        this.rangeEmoji = null;
        this.radiusEmoji = new HashMap<>();

        this.knownVersions = this.client.getGameVersions();
        ClassJobCategoryJobs jobs = this.client.getById(ClassJobCategoryJobs.class, JOBS_CATEGORY_ROW_ID);
        for(String job : jobs.getJobs().keySet()) {
            if(jobs.getJobs().get(job)) knownJobs.add(job);
        }

        this.client.getListIterator(ClassJob.class).stream().forEach(classJob -> {
            if(classJob.getAbbreviation() != null && !classJob.getAbbreviation().isEmpty()) {
                this.allJobs.put(classJob.getAbbreviation(), classJob);
            }
        });
    }

    @Override
    public String id() {
        return XivActionCommand.ID;
    }

    @Override
    public ApplicationCommandRequest register(String id) {
        ApplicationCommandRequest r = ApplicationCommandRequest.builder()
                .name(id)
                .nameLocalizationsOrNull(Map.of(
                        "ja", "xivaction"
                ))
                .description("Display information for FFXIV Action")
                .descriptionLocalizationsOrNull(Map.of(
                        "ja", "FFXIVアクションの情報を表示"
                ))
                .addOption(ApplicationCommandOptionData.builder()
                        .name("job")
                        .nameLocalizationsOrNull(Map.of(
                                "ja", "ジョブ"
                        ))
                        .description("Job to display info for")
                        .descriptionLocalizationsOrNull(Map.of(
                                "ja", "アクションが検索できるジョブ"
                        ))
                        .required(true)
                        .type(3)
                        .choices(this.knownJobs.stream().map(v ->
                                ApplicationCommandOptionChoiceData.builder()
                                        .name(v)
                                        .value(v)
                                        .build()).toList())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("action")
                        .nameLocalizationsOrNull(Map.of(
                                "ja", "アクション"
                        ))
                        .description("Action to display info for")
                        .descriptionLocalizationsOrNull(Map.of(
                                "ja", "情報が表示されるアクション"
                        ))
                        .required(true)
                        .type(4)
                        .autocomplete(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("language")
                        .nameLocalizationsOrNull(Map.of(
                                "ja", "言語"
                        ))
                        .description("Language to display Action data in")
                        .descriptionLocalizationsOrNull(Map.of(
                                "ja", "アクション情報が表示する言語"
                        ))
                        .required(false)
                        .type(3)
                        .choices(
                                ApplicationCommandOptionChoiceData.builder()
                                        .name("English")
                                        .nameLocalizationsOrNull(Map.of(
                                                "ja", "英語"
                                        ))
                                        .value("en")
                                        .build(),
                                ApplicationCommandOptionChoiceData.builder()
                                        .name("Japanese")
                                        .nameLocalizationsOrNull(Map.of(
                                                "ja", "日本語"
                                        ))
                                        .value("ja")
                                        .build(),
                                ApplicationCommandOptionChoiceData.builder()
                                        .name("French")
                                        .nameLocalizationsOrNull(Map.of(
                                                "ja", "フランス語"
                                        ))
                                        .value("fr")
                                        .build(),
                                ApplicationCommandOptionChoiceData.builder()
                                        .name("German")
                                        .nameLocalizationsOrNull(Map.of(
                                                "ja", "ドイツ語"
                                        ))
                                        .value("de")
                                        .build()
                        )
                        .build())
//                .addOption(ApplicationCommandOptionData.builder()
//                        .name("version")
//                        .description("Game version")
//                        .required(false)
//                        .type(3)
//                        .choices(this.knownVersions.stream().map(v ->
//                                ApplicationCommandOptionChoiceData.builder()
//                                        .name(v)
//                                        .value(v)
//                                        .build()).toList())
//                        .build())
                .addAllIntegrationTypes(List.of(0, 1))
                .addAllContexts(List.of(0, 1, 2))
                .dmPermission(true)
                .build();

        return r;
    }

    @Override
    public Mono<Void> handleAutoComplete(FishUtilities client, ChatInputAutoCompleteEvent event) {
        Optional<String> job = event.getOption("job")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(String::toUpperCase);
        if("action".equals(event.getFocusedOption().getName()) && job.stream().anyMatch(this.knownJobs::contains)) {
            SearchFilter actionsForJob = SearchFilters.and(
                    SearchFilters.eq("ClassJobCategory.%s".formatted(job.get()), true),
                    SearchFilters.eq("ClassJobCategory.ADV", false),
                    SearchFilters.gt("ClassJobLevel", 0)
            );
            SearchFilter queryForUser;
            if(event.getFocusedOption().getValue().stream().anyMatch(v -> !v.getRaw().isEmpty())) {
                SearchFilter languageMatching = SearchFilters.or(
                        SearchFilters.strPart("Name@en", event.getFocusedOption().getValue().get().getRaw()),
                        SearchFilters.strPart("Name@de", event.getFocusedOption().getValue().get().getRaw()),
                        SearchFilters.strPart("Name@fr", event.getFocusedOption().getValue().get().getRaw()),
                        SearchFilters.strPart("Name@ja", event.getFocusedOption().getValue().get().getRaw())
                );
                queryForUser = SearchFilters.and(
                        actionsForJob,
                        languageMatching
                );
            } else {
                queryForUser = actionsForJob;
            }

            // TODO check version field
            XivApiPaginator<SearchAction> actions = this.client.getSearchIterator(SearchAction.class, queryForUser,
                    ListOptions.newBuilder().perPage(25).build());

            // TODO check language field
            String language = event.getOption("language")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .orElse(event.getInteraction().getUserLocale());

            String nameIndexer = XivApiUtil.discordLocaleToXiv(language);

            List<ApplicationCommandOptionChoiceData> optionsList = actions.stream()
                    .limit(25)
                    .<ApplicationCommandOptionChoiceData>map(action -> ApplicationCommandOptionChoiceData.builder()
                            .name(action.getName().get(nameIndexer))
                            .value(action.getRowId())
                            .build())
                    .toList();
            return event.respondWithSuggestions(optionsList).doOnError(Throwable::printStackTrace);
        } else return Mono.empty();
    }

    @Override
    public Mono<Void> handleSlashCommand(FishUtilities client, ChatInputInteractionEvent event) {
        var a = event.deferReply();
        Mono<Void> b = Mono.defer(() -> {
            try {
                return followup(client, event);
            } catch (Exception e) {
                e.printStackTrace();
                return event.editReply(":(").then();
            }
        });
        return a.then(b);
    }

    public Mono<Void> followup(FishUtilities client, ChatInputInteractionEvent event) {
        // TODO think about if I want this to all use row IDs
        String job = event.getOption("job")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(String::toUpperCase)
                .get();

        ClassJob jobObject = this.allJobs.get(job);

        String language = event.getOption("language")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(event.getInteraction().getUserLocale());

        String version = event.getOption("version")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(null);

        int actionId = (int) (long) event.getOption("action")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .get();

        Action action = this.client.getById(Action.class, actionId);
        ClassJobCategoryJobs jobs = action.getClassJobCategory();
        ClassJobCategoryName classJobCategoryName = this.client.getById(ClassJobCategoryName.class, jobs.getRowId());

        String xivLanguage = XivApiUtil.discordLocaleToXiv(language);

        String descriptionAnsi = DiscordColorFormatter.toAnsi(action.getDescriptionHtml().get(xivLanguage));

        Flux<CustomEmoji> emoji = Flux.fromIterable(this.knownJobs).flatMap(j -> {
            if(jobs.getJobs().get(j)) {
                if(this.emojiCache.containsKey(j)) {
                    return Mono.just(this.emojiCache.get(j));
                } else {
                    ClassJob cj = this.allJobs.get(j);
                    Mono<CustomEmoji> ce = XivApiUtil.getOrAddJobIconEmoji(client, this.client, cj);
                    return ce.doOnNext(e -> this.emojiCache.put(j, e));
                }
            } else {
                return Flux.empty();
            }
        });
        return populateEmotes(client).thenMany(emoji).collectList().flatMap(emojiList -> {
            StringBuilder infoString = new StringBuilder();
            infoString.append("-# ");
            for(CustomEmoji ce : emojiList) {
                infoString.append(ce.asFormat());
            }
            infoString.append(" ");
            infoString.append(classJobCategoryName.getName().get(xivLanguage));

            if(action.getCastType() < CastType.values().length) {
                infoString.append("\n");

                infoString.append(this.rangeEmoji.asFormat());
                infoString.append(" ");
                infoString.append(action.getRange());
                infoString.append("y");

                CastType castType = CastType.values()[action.getCastType()];
                infoString.append(" ");
                infoString.append(this.radiusEmoji.get(castType).asFormat());
                infoString.append(" ");
                infoString.append(action.getEffectRange());
                infoString.append("y");
            }

            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .addComponent(Container.of(
                            Section.of(
                                    Thumbnail.of(UnfurledMediaItem.of(action.getIcon().getAssetPathHd()
                                            .getURI(ImageFormat.PNG).toString())),
                                    TextDisplay.of("**%s**".formatted(action.getName().get(xivLanguage))),
                                    TextDisplay.of("```ansi\n%s\n```".formatted(descriptionAnsi)),
                                    TextDisplay.of(infoString.toString())
                            )
                    ))
                    .build()).then();
        });
    }

    private Mono<Void> populateEmotes(FishUtilities client) {
        return client.gateway.getApplicationInfo().flatMap(ai -> {
            Mono<Void> populateEmojiCache = Mono.empty();
            if(this.emojiCache.isEmpty()) {
                populateEmojiCache = ai.getEmojis().doOnNext(ae -> {
                            String upperName = ae.getName().toUpperCase(Locale.US);
                            if(this.knownJobs.contains(upperName)) {
                                this.emojiCache.put(upperName, ae);
                            }
                            if("range".equals(ae.getName())) {
                                this.rangeEmoji = ae;
                            }
                            try {
                                CastType ct = CastType.valueOf(upperName);
                                this.radiusEmoji.put(ct, ae);
                            } catch (IllegalArgumentException ignored) {}
                        }).then();
            }

            Mono<Void> createRange = Mono.defer(() -> {
                if(this.rangeEmoji == null) {
                    byte[] pngBytes;
                    try(InputStream is = XivActionCommand.class.getResourceAsStream("range.png")) {
                        pngBytes = is.readAllBytes();
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                    return ai.createEmoji("range", Image.ofRaw(pngBytes, Image.Format.PNG))
                            .doOnNext(ae -> this.rangeEmoji = ae);
                } else {
                    return Mono.empty();
                }
            }).then();

            Mono<Void> createRadius = Flux.fromArray(CastType.values()).flatMap(ct -> {
                if(!this.radiusEmoji.containsKey(ct)) {
                    byte[] pngBytes;
                    String emojiName = ct.name().toLowerCase(Locale.US);
                    try(InputStream is = XivActionCommand.class.getResourceAsStream(emojiName + ".png")) {
                        pngBytes = is.readAllBytes();
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                    return ai.createEmoji(emojiName, Image.ofRaw(pngBytes, Image.Format.PNG))
                            .map(ae -> Tuples.of(ct, ae));
                } else {
                    return Mono.empty();
                }
            }).doOnNext(t -> this.radiusEmoji.put(t.getT1(), t.getT2())).then();

            return populateEmojiCache.then(createRange).then(createRadius);
        });
    }
}
