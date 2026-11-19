package cn.charlotte.pit.database;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.ActivityData;
import cn.charlotte.pit.data.*;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.Getter;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.mongojack.JacksonMongoCollection;
import org.slf4j.Logger;

/**
 * 2 * @Author: EmptyIrony
 * 3 * @Date: 2020/12/28 23:03
 * 4
 */
@Getter
public class MongoDB {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MongoDB.class);

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> itemUuidBlacklistCollection;
    private MongoCollection<Document> itemUuidConfiscationCollection;
    private JacksonMongoCollection<PlayerProfile> profileCollection;
    private JacksonMongoCollection<TradeData> tradeCollection;
    private JacksonMongoCollection<PlayerMailData> mailCollection;
    private JacksonMongoCollection<PlayerInvBackup> invCollection;
    private JacksonMongoCollection<CDKData> cdkCollection;
    private JacksonMongoCollection<FixedRewardData> rewardCollection;
    private JacksonMongoCollection<ActivityData> activityDataCollection;

    private JacksonMongoCollection<EventQueue> eventQueueCollection;
    private @Getter JacksonMongoCollection<CustomPaperData> customPaperCollection;

    public void connect() {
        log.info("Connecting to database...");

        String address = ThePit.getInstance().getPitConfig().getMongoDBAddress();
        int port = ThePit.getInstance().getPitConfig().getMongoDBPort();

        final String mongoUser = ThePit.getInstance().getPitConfig().getMongoUser();
        final String mongoPassword = ThePit.getInstance().getPitConfig().getMongoPassword();

        final String databaseName;
        if (ThePit.getInstance().getPitConfig().getDatabaseName() == null) {
            databaseName = "thePit";
        } else {
            databaseName = ThePit.getInstance().getPitConfig().getDatabaseName();
        }

        final ServerAddress serverAddress = new ServerAddress(address, port);
        if (mongoUser != null && mongoPassword != null) {
            final MongoCredential credential = MongoCredential.createCredential(mongoUser, databaseName, mongoPassword.toCharArray());

            this.mongoClient = new MongoClient(serverAddress, credential, new MongoClientOptions.Builder().build());
        } else {
            this.mongoClient = new MongoClient(serverAddress);
        }

        this.database = mongoClient.getDatabase(databaseName);
        this.collection = database.getCollection("players");
        this.itemUuidBlacklistCollection = database.getCollection("item_uuid_blacklist");
        this.itemUuidConfiscationCollection = database.getCollection("item_uuid_confiscations");

        this.profileCollection = JacksonMongoCollection.builder().build(this.database.getCollection("players", PlayerProfile.class), PlayerProfile.class, UuidRepresentation.JAVA_LEGACY);

        this.tradeCollection = JacksonMongoCollection.builder().build(this.database.getCollection("trade", TradeData.class), TradeData.class, UuidRepresentation.JAVA_LEGACY);

        this.mailCollection = JacksonMongoCollection.builder().build(this.database.getCollection("mail", PlayerMailData.class), PlayerMailData.class, UuidRepresentation.JAVA_LEGACY);

        this.invCollection = JacksonMongoCollection.builder().build(this.database.getCollection("inv", PlayerInvBackup.class), PlayerInvBackup.class, UuidRepresentation.JAVA_LEGACY);

        this.cdkCollection = JacksonMongoCollection.builder().build(this.database.getCollection("cdk", CDKData.class), CDKData.class, UuidRepresentation.JAVA_LEGACY);

        this.rewardCollection = JacksonMongoCollection.builder().build(this.database.getCollection("reward", FixedRewardData.class), FixedRewardData.class, UuidRepresentation.JAVA_LEGACY);

        this.eventQueueCollection = JacksonMongoCollection.builder().build(this.database.getCollection("event_queue", EventQueue.class), EventQueue.class, UuidRepresentation.JAVA_LEGACY);

        this.customPaperCollection = JacksonMongoCollection.builder().build(this.database.getCollection("custom_paper", CustomPaperData.class), CustomPaperData.class, UuidRepresentation.JAVA_LEGACY);

        this.activityDataCollection = JacksonMongoCollection.builder().build(this.database.getCollection("activityData", ActivityData.class), ActivityData.class, UuidRepresentation.JAVA_LEGACY);
//        createIndex(mailCollection, "uuidIndex", "uuid");

        // Index inspection/building can take minutes on a large collection.
        // Never hold the Minecraft server thread while MongoDB performs it.
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), this::ensureIndexes);

        log.info("Connected!");

//        log.info("loading cdk...");
//        CDKData.loadAllCDKFromData();
//        log.info("loaded!");
    }


    private void createIndex(MongoCollection<Document> collection, String indexName, String fieldName) {
        createIndex(collection, indexName, Indexes.ascending(fieldName));
    }

    private void createIndex(MongoCollection<Document> collection, String indexName, Bson index) {
        try {
            String existingIndexName = findIndexByDefinition(collection, index);
            if (existingIndexName != null) {
                log.info("MongoDB index ready: {} (using existing index {})", indexName, existingIndexName);
                return;
            }

            collection.createIndex(index, new IndexOptions().name(indexName));
            log.info("MongoDB index ready: {}", indexName);
        } catch (MongoCommandException exception) {
            // Multiple servers may race between listIndexes and createIndex.
            // If the requested key definition now exists under any name, it is usable.
            String existingIndexName = findIndexByDefinition(collection, index);
            if (existingIndexName != null) {
                log.info("MongoDB index ready: {} (using concurrently-created index {})",
                        indexName, existingIndexName);
                return;
            }
            log.warn("Unable to create MongoDB index {} (error code {})",
                    indexName, exception.getErrorCode(), exception);
        } catch (Exception exception) {
            log.warn("Unable to create MongoDB index {}", indexName, exception);
        }
    }

    private void ensureIndexes() {
        MongoCollection<Document> playerCollection = database.getCollection("players");
        createIndex(playerCollection, "uuidIndex", "uuid");
        createIndex(playerCollection, "lowerNameIndex", "lowerName");

        MongoCollection<Document> tradeDocuments = database.getCollection("trade");
        createIndex(tradeDocuments, "playerAIndex", "playerA");
        createIndex(tradeDocuments, "playerBIndex", "playerB");
        createIndex(tradeDocuments, "tradeUuidIndex", "tradeUuid");
        createIndex(tradeDocuments, "completeTimeIndex", "completeTime");
        createIndex(tradeDocuments, "timeStampIndex", "timeStamp");

        MongoCollection<Document> inventoryBackups = database.getCollection("inv");
        createIndex(inventoryBackups, "uuidIndex", "uuid");
        createIndex(inventoryBackups, "uuidTimeStampIndex", Indexes.ascending("uuid", "timeStamp"));
        createIndex(inventoryBackups, "backupUuidIndex", "backupUuid");

        createIndex(itemUuidBlacklistCollection, "itemUuidBlacklistIndex", "itemUuid");
        createIndex(itemUuidConfiscationCollection, "confiscationPlayerTimeIndex",
                Indexes.ascending("playerUuid", "confiscatedAt"));
        createIndex(itemUuidConfiscationCollection, "confiscationItemUuidIndex", "itemUuid");
    }

    private String findIndexByDefinition(MongoCollection<Document> collection, Bson requestedIndex) {
        try {
            BsonDocument requestedKeys = requestedIndex.toBsonDocument(
                    Document.class,
                    collection.getCodecRegistry()
            );
            for (Document existingIndex : collection.listIndexes()) {
                Document existingKeys = existingIndex.get("key", Document.class);
                if (existingKeys == null) {
                    continue;
                }
                BsonDocument existingDefinition = existingKeys.toBsonDocument(
                        Document.class,
                        collection.getCodecRegistry()
                );
                if (requestedKeys.equals(existingDefinition)) {
                    return existingIndex.getString("name");
                }
            }
        } catch (Exception exception) {
            log.debug("Unable to inspect existing MongoDB indexes", exception);
        }
        return null;
    }
}
