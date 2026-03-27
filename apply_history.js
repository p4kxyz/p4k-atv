const fs = require('fs');

// 1. Update WatchHistorySyncManager.java
let managerPath = 'app/src/main/java/com/files/codes/utils/sync/WatchHistorySyncManager.java';
let managerContent = fs.readFileSync(managerPath, 'utf8');

let newMethod = `    /**\n     * Xóa một item cụ thể trong lịch sử xem\n     */\n    public void deleteWatchHistoryItem(String itemId, SyncCallback callback) {\n        // 1. Xóa ở local\n        Map<String, WatchHistorySyncItem.WatchHistoryItem> localHistory = getLocalWatchHistory();\n        if (localHistory != null && localHistory.containsKey(itemId)) {\n            localHistory.remove(itemId);\n            saveLocalWatchHistory(localHistory);\n        }\n\n        // 2. Xóa trên server nếu đang đăng nhập\n        if (canAutoSync() && getSyncUserId() != null) {\n            String userId = getSyncUserId();\n            Call<JsonObject> call = syncService.deleteWatchHistoryItem(userId, itemId);\n            call.enqueue(new Callback<JsonObject>() {\n                @Override\n                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {\n                    if (callback != null) {\n                        if (response.isSuccessful() && response.body() != null) {\n                            callback.onSuccess("Item deleted successfully");\n                        } else {\n                            callback.onError("Failed to delete item from server");\n                        }\n                    }\n                }\n                @Override\n                public void onFailure(Call<JsonObject> call, Throwable t) {\n                    if (callback != null) {\n                        callback.onError("Network error: " + t.getMessage());\n                    }\n                }\n            });\n        } else {\n            if (callback != null) {\n                callback.onSuccess("Item deleted locally (not synced to server)");\n            }\n        }\n    }\n\n    public void clearAllWatchHistory`;

managerContent = managerContent.replace('public void clearAllWatchHistory', newMethod);
fs.writeFileSync(managerPath, managerContent, 'utf8');


// 2. Update WatchHistoryPageFragment.java
let fragmentPath = 'app/src/main/java/com/files/codes/view/fragments/WatchHistoryPageFragment.java';
let fragmentContent = fs.readFileSync(fragmentPath, 'utf8');

let oldListenerStart = `    // Click → play with resume
    private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return (viewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof VideoContent)) return;
            VideoContent vc = (VideoContent) item;

            // Parse position from description
            long currentPosition = 0;
            String desc = vc.getDescription();
            if (desc != null && desc.startsWith("WATCH_HISTORY:")) {
                try {
                    currentPosition = Long.parseLong(desc.substring("WATCH_HISTORY:".length()));
                } catch (NumberFormatException e) {
                    currentPosition = 0;
                }
            }

            String contentType = "movie";
            if (vc.getIsTvseries() != null && vc.getIsTvseries().equals("1")) {
                contentType = "tvseries";
            } else if (vc.getType() != null) {
                contentType = vc.getType();
            }

            String videoUrl = vc.getVideoUrl();
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                // No URL → go to details page
                Intent intent = new Intent(getActivity(), HeroStyleVideoDetailsActivity.class);
                intent.putExtra("id", vc.getId());
                intent.putExtra("type", contentType);
                intent.putExtra("thumbImage", vc.getThumbnailUrl() != null ? vc.getThumbnailUrl() : "");
                startActivity(intent);
                return;
            }

            // Go directly to player
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putExtra("id", vc.getId());
            intent.putExtra("type", contentType);
            intent.putExtra("title", vc.getTitle());
            intent.putExtra("poster", vc.getPosterUrl());
            intent.putExtra("thumbnail", vc.getThumbnailUrl());
            intent.putExtra("video_url", videoUrl);
            intent.putExtra("position", currentPosition);
            intent.putExtra("from_watch_history", true);
            startActivity(intent);
        };
    }`;

// convert formatting specifically ignoring exact indents matching
let regexOldListener = /private OnItemViewClickedListener getDefaultItemViewClickedListener\(\) \{[\s\S]*?startActivity\(intent\);\s*\};\s*\}/m;

let newListener = `private OnItemViewClickedListener getDefaultItemViewClickedListener() {
        return (viewHolder, item, rowViewHolder, row) -> {
            if (!(item instanceof VideoContent)) return;
            VideoContent vc = (VideoContent) item;

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext(), android.R.style.Theme_Device_Default_Dialog_Alert);
            builder.setTitle(vc.getTitle());
            String[] options = {"Xem tiếp", "Xóa khỏi lịch sử"};
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    playVideo(vc);
                } else if (which == 1) {
                    deleteHistoryItem(vc);
                }
            });
            builder.show();
        };
    }

    private void playVideo(VideoContent vc) {
        long currentPosition = 0;
        String desc = vc.getDescription();
        if (desc != null && desc.startsWith("WATCH_HISTORY:")) {
            try {
                currentPosition = Long.parseLong(desc.substring("WATCH_HISTORY:".length()));
            } catch (NumberFormatException e) {
                currentPosition = 0;
            }
        }

        String contentType = "movie";
        if (vc.getIsTvseries() != null && vc.getIsTvseries().equals("1")) {
            contentType = "tvseries";
        } else if (vc.getType() != null) {
            contentType = vc.getType();
        }

        String videoUrl = vc.getVideoUrl();
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            Intent intent = new Intent(getActivity(), HeroStyleVideoDetailsActivity.class);
            intent.putExtra("id", vc.getId());
            intent.putExtra("type", contentType);
            intent.putExtra("thumbImage", vc.getThumbnailUrl() != null ? vc.getThumbnailUrl() : "");
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(getActivity(), PlayerActivity.class);
        intent.putExtra("id", vc.getId());
        intent.putExtra("type", contentType);
        intent.putExtra("title", vc.getTitle());
        intent.putExtra("poster", vc.getPosterUrl());
        intent.putExtra("thumbnail", vc.getThumbnailUrl());
        intent.putExtra("video_url", videoUrl);
        intent.putExtra("position", currentPosition);
        intent.putExtra("from_watch_history", true);
        startActivity(intent);
    }

    private void deleteHistoryItem(VideoContent vc) {
        if (syncManager != null) {
            syncManager.deleteWatchHistoryItem(vc.getId(), new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Đã xóa khỏi lịch sử", Toast.LENGTH_SHORT).show();
                            loadWatchHistory(); // Reload to refresh grid
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Lỗi khi xóa: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }`;

if (regexOldListener.test(fragmentContent)) {
    fragmentContent = fragmentContent.replace(regexOldListener, newListener);
    fs.writeFileSync(fragmentPath, fragmentContent, 'utf8');
    console.log("Success updated Fragment");
} else {
    console.log("Failed to match old listener regex");
}
