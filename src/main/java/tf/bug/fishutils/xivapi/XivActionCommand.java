package tf.bug.fishutils.xivapi;

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gg.xp.xivapi.XivApiClient;
import gg.xp.xivapi.assets.ImageFormat;
import gg.xp.xivapi.filters.SearchFilter;
import gg.xp.xivapi.filters.SearchFilters;
import gg.xp.xivapi.pagination.ListOptions;
import gg.xp.xivapi.pagination.XivApiPaginator;
import java.util.*;
import java.util.function.Function;
import reactor.core.publisher.Mono;
import tf.bug.fishutils.Command;
import tf.bug.fishutils.FishUtilities;
import tf.bug.fishutils.xivapi.model.Action;
import tf.bug.fishutils.xivapi.model.ClassJob;
import tf.bug.fishutils.xivapi.model.SearchAction;
import tf.bug.fishutils.xivapi.model.ClassJobCategory;

public class XivActionCommand extends Command {
    public static final String ID = "xivaction";

    /// The spreadsheet row in `ClassJobCategory` corresponding to end-game jobs
    public static final int JOBS_CATEGORY_ROW_ID = 85;

    private final XivApiClient client;
    private final List<String> knownVersions;
    private final SortedSet<String> knownJobs;
    private final Map<String, ClassJob> allJobs;

    public XivActionCommand(XivApiClient client) {
        this.client = client;
        this.knownJobs = new TreeSet<>();
        this.allJobs = new HashMap<>();

        this.knownVersions = this.client.getGameVersions();
        ClassJobCategory jobs = this.client.getById(ClassJobCategory.class, JOBS_CATEGORY_ROW_ID);
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
            List<ApplicationCommandOptionChoiceData> optionsList = actions.stream()
                    .limit(25)
                    .<ApplicationCommandOptionChoiceData>map(action -> ApplicationCommandOptionChoiceData.builder()
                            .name(action.getName().getEn())
                            .nameLocalizationsOrNull(Map.of(
                                    "en-US", action.getName().getEn(),
                                    "ja", action.getName().getJp(),
                                    "fr", action.getName().getFr(),
                                    "de", action.getName().getDe()
                            ))
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

        String name;
        String descriptionHtml;
        switch(language) {
            case "en", "en-US", "en-GB" -> {
                name = action.getName().getEn();
                descriptionHtml = action.getDescriptionHtml().getEn();
            }
            case "ja" -> {
                name = action.getName().getJp();
                descriptionHtml = action.getDescriptionHtml().getJp();
            }
            case "fr" -> {
                name = action.getName().getFr();
                descriptionHtml = action.getDescriptionHtml().getFr();
            }
            case "de" -> {
                name = action.getName().getDe();
                descriptionHtml = action.getDescriptionHtml().getDe();
            }
            default -> {
                name = action.getName().getEn();
                descriptionHtml = action.getDescriptionHtml().getEn();
            }
        }

        String descriptionAnsi = DiscordColorFormatter.toAnsi(descriptionHtml);

        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .author(
                                        name,
                                        null,
                                        XivApiUtil.getBorderlessJobIcon(this.client, jobObject, ImageFormat.PNG).toString())
                                .description("```ansi\n%s\n```".formatted(descriptionAnsi))
                                .thumbnail(action.getIcon().getAssetPathHd().getURI(ImageFormat.PNG).toString())
                                .build())
                .build()).then();
    }
}
