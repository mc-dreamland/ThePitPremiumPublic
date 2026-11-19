package cn.charlotte.pit.commands.admin;

record MaterialHistoryBackupReference(String databaseId,
                                      String backupUuid,
                                      String playerUuid,
                                      String playerName,
                                      int prestige,
                                      long timestamp,
                                      String time) {
}
