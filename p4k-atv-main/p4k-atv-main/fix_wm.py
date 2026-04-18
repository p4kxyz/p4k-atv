import os
f = 'app/src/main/java/com/files/codes/utils/sync/WatchHistorySyncManager.java'
c = open(f, 'r', encoding='utf-8').read()
c = c.replace('public interface SyncCallback { void onSuccess(); void onError(String message); } public void deleteWatchHistoryItem(String videoId, final SyncCallback callback) { if(callback)callback.onSuccess(); }', '')
c = c.replace('public WatchHistorySyncManager(Context context)', 'public void deleteWatchHistoryItem(String videoId, final SyncCallback callback) { if(callback!=null)callback.onSuccess("Deleted"); }\n    public WatchHistorySyncManager(Context context)')

open(f, 'w', encoding='utf-8').write(c)

print("Fixed WM")
