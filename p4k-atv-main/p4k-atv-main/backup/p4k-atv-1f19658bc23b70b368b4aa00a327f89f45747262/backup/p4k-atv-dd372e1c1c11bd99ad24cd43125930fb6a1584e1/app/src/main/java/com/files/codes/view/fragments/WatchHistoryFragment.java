package com.files.codes.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.HorizontalGridView;

import com.files.codes.R;
import com.files.codes.adapters.WatchHistoryAdapter;
import com.files.codes.model.sync.WatchHistorySyncItem;
import com.files.codes.utils.sync.WatchHistorySyncManager;
import com.files.codes.view.dialog.SyncSetupDialog;
import com.files.codes.view.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment hiển thị lịch sử xem với API-only approach
 * - Mọi dữ liệu đều được load trực tiếp từ server
 * - Không lưu trữ local storage (chỉ dùng tạm cho sync)
 * - Yêu cầu đăng nhập để truy cập lịch sử
 * - Mỗi lần mở fragment đều fetch fresh data từ API
 */
public class WatchHistoryFragment extends Fragment {
    
    private static final String TAG = "WatchHistoryFragment";
    
    private HorizontalGridView historyGridView;
    private HorizontalGridView recyclerWatchHistory;
    private WatchHistoryAdapter adapter;
    private Button btnSync, btnRefresh, btnClearAll;
    private TextView tvSyncStatus, tvEmptyMessage, tvEmptyState, tvLastSync;
    private ProgressBar progressBar;
    
    private WatchHistorySyncManager syncManager;
    private List<WatchHistorySyncItem.WatchHistoryItem> historyItems = new ArrayList<>();
    private String currentUserEmail = null; // Track current user for user switching detection

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watch_history, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupButtons();
        
        syncManager = WatchHistorySyncManager.getInstance(getContext());
        
        loadWatchHistory();
        updateSyncStatus();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("WatchHistory", "📱 onResume() called");
        
        // Check if user has switched accounts
        String newUserEmail = syncManager.getCurrentUserEmail();
        android.util.Log.d("WatchHistory", "👤 Current tracked user: " + currentUserEmail + ", New user: " + newUserEmail);
        
        boolean userSwitched = (currentUserEmail == null && newUserEmail != null) ||
                              (currentUserEmail != null && !currentUserEmail.equals(newUserEmail));
        
        if (userSwitched) {
            // User switched - force clear all cached data including SharedPreferences
            android.util.Log.d("WatchHistory", "🔄 User switched in onResume: " + currentUserEmail + " → " + newUserEmail);
            forceClearSharedPreferencesCache();
            
            historyItems.clear();
            if (adapter != null) {
                adapter.updateItems(historyItems);
            }
            currentUserEmail = newUserEmail;
        }
        
        // Always refresh from server when fragment resumes
        updateSyncStatus();
        loadWatchHistory();
    }
    
    private void initViews(View view) {
        recyclerWatchHistory = view.findViewById(R.id.recycler_watch_history);
        btnSync = view.findViewById(R.id.btn_sync);
        btnClearAll = view.findViewById(R.id.btn_clear_all);
        tvSyncStatus = view.findViewById(R.id.tv_sync_status);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvLastSync = view.findViewById(R.id.tv_last_sync);
        progressBar = view.findViewById(R.id.progress_bar);
    }
    
    private void setupRecyclerView() {
        adapter = new WatchHistoryAdapter(getContext());
        adapter.setOnItemClickListener(item -> {
            // Open player with watch history data
            android.util.Log.d("WatchHistory", "🎬 Opening player from watch history - ID: " + item.getVideoId() + ", Position: " + item.getPosition());
            
            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra("id", item.getVideoId());
            intent.putExtra("type", item.getVideoType());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("poster", item.getPosterUrl());
            intent.putExtra("thumbnail", item.getThumbnailUrl());
            intent.putExtra("video_url", item.getVideoUrl()); // Required for direct playback
            intent.putExtra("position", (int) item.getPosition()); // Resume from last position
            intent.putExtra("from_watch_history", true); // Flag to enable back to VideoDetailsActivity
            
            startActivity(intent);
        });
        
        recyclerWatchHistory.setAdapter(adapter);
    }
    
    private void setupButtons() {
        btnSync.setOnClickListener(v -> {
            showSyncOptions();
        });

        btnRefresh.setOnClickListener(v -> {
            loadWatchHistory();
        });

        btnClearAll.setOnClickListener(v -> {
            showClearAllConfirmDialog();
        });
    }

    private void showSyncOptions() {
        // Đơn giản hóa - chỉ có 3 option chính
        String[] options = {
            "🔄 Đồng bộ lại từ server", 
            "🗑️ Xóa tất cả lịch sử",
            "🧹 Xóa cache (Testing)",
            "ℹ️ Show User Info (Debug)"
        };
        
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Quản lý lịch sử xem")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Đồng bộ lại từ server
                            manualSyncFromServer();
                            break;
                        case 1:
                            // Xóa tất cả (cả local và server)
                            confirmDeleteAllHistory();
                            break;
                        case 2:
                            // Xóa cache cho testing
                            manualClearCache();
                            break;
                        case 3:
                            // Show user debug info
                            showUserDebugInfo();
                            break;
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    /**
     * Đồng bộ thủ công từ server (khi người dùng nhấn nút)
     */
    private void manualSyncFromServer() {
        if (!syncManager.canAutoSync()) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgress(true);
        syncManager.createAutoSyncLink(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                syncFromServerInternal();
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), "Lỗi đồng bộ: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    /**
     * Xóa cache thủ công để testing user switching
     */
    private void manualClearCache() {
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("🧹 Xóa Cache Testing")
                .setMessage("Xóa cache SharedPreferences để test user switching?\n\n⚠️ Chỉ dùng để testing!")
                .setPositiveButton("Xóa Cache", (dialog, which) -> {
                    forceClearSharedPreferencesCache();
                    // Reload lại để thấy effect
                    loadWatchHistory();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    /**
     * Show user debug info cho troubleshooting
     */
    private void showUserDebugInfo() {
        String currentUser = syncManager.getCurrentUserEmail();
        String trackedUser = currentUserEmail;
        
        // Read SharedPreferences to see what's cached
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("watch_history_sync", 0);
        String cachedUserId = prefs.getString("sync_user_id", "null");
        String cachedEmail = prefs.getString("sync_email", "null");
        String cachedHistory = prefs.getString("local_watch_history", "null");
        boolean hasCache = cachedHistory != null && !cachedHistory.equals("null") && !cachedHistory.isEmpty();
        
        String debugInfo = "🔍 DEBUG USER INFO:\n\n" +
                          "Current User (SyncManager): " + (currentUser != null ? currentUser : "null") + "\n" +
                          "Tracked User (Fragment): " + (trackedUser != null ? trackedUser : "null") + "\n\n" +
                          "SharedPreferences Cache:\n" +
                          "• User ID: " + cachedUserId + "\n" +
                          "• Email: " + cachedEmail + "\n" +
                          "• Has History Cache: " + hasCache + "\n" +
                          "• History Length: " + (hasCache ? cachedHistory.length() + " chars" : "0");
        
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("🐛 User Debug Info")
                .setMessage(debugInfo)
                .setPositiveButton("OK", null)
                .setNeutralButton("Copy to Log", (dialog, which) -> {
                    android.util.Log.d("WatchHistory", debugInfo);
                    Toast.makeText(getContext(), "Debug info copied to logcat", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    
    /**
     * Xác nhận xóa tất cả lịch sử (cả local và server)
     */
    private void confirmDeleteAllHistory() {
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("⚠️ Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa TOÀN BỘ lịch sử xem?\n\n" +
                           "• Xóa trên thiết bị này\n" +
                           "• Xóa trên server (tất cả thiết bị)\n\n" +
                           "Hành động này KHÔNG THỂ hoàn tác!")
                .setPositiveButton("🗑️ Xóa tất cả", (dialog, which) -> {
                    deleteAllHistory();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    /**
     * Xóa tất cả lịch sử (cả local và server)
     */
    private void deleteAllHistory() {
        showProgress(true);
        
        if (syncManager.canAutoSync()) {
            // Xóa trên server trước
            syncManager.clearServerWatchHistory(new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    // Sau đó xóa local
                    clearLocalHistory();
                }

                @Override
                public void onError(String error) {
                    // Dù server lỗi vẫn xóa local
                    clearLocalHistory();
                }
            });
        } else {
            // Chỉ xóa local
            clearLocalHistory();
        }
    }
    
    /**
     * Xóa lịch sử local
     */
    private void clearLocalHistory() {
        syncManager.clearLocalWatchHistory(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), "✅ Đã xóa tất cả lịch sử", Toast.LENGTH_SHORT).show();
                        loadWatchHistory(); // Reload UI
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), "Lỗi xóa local: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void showAutoSyncDialog(String email) {
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Đồng bộ lịch sử xem")
                .setMessage("Bạn đã đăng nhập với email: " + email + 
                           "\n\nBạn muốn đồng bộ lịch sử xem với thiết bị khác?")
                .setPositiveButton("Đồng bộ lên server", (dialog, which) -> {
                    autoSyncToServer();
                })
                .setNeutralButton("Tải về từ server", (dialog, which) -> {
                    autoSyncFromServer();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showManualSyncSetup() {
        String[] options = {
            "Đồng bộ lên server", 
            "Đồng bộ từ server về", 
            "Cài đặt đồng bộ", 
            "Xóa dữ liệu server"
        };
        
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Tùy chọn đồng bộ")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            syncToServer();
                            break;
                        case 1:
                            syncFromServer();
                            break;
                        case 2:
                            showSyncSetupDialog();
                            break;
                        case 3:
                            clearServerData();
                            break;
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showSyncSetupDialog() {
        SyncSetupDialog dialog = new SyncSetupDialog(getContext(), new SyncSetupDialog.OnSyncSetupListener() {
            @Override
            public void onSetupComplete(String email) {
                setupSync(email);
            }
        });
        dialog.show();
    }
    
    private void autoSyncToServer() {
        showProgress(true);
        syncManager.createAutoSyncLink(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                syncToServerInternal();
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void autoSyncFromServer() {
        showProgress(true);
        syncManager.createAutoSyncLink(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                syncFromServerInternal();
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void setupSync(String email) {
        showProgress(true);
        syncManager.createSyncLink(email, new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        updateSyncStatus();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void syncToServer() {
        if (!syncManager.isSyncConfigured()) {
            showSyncSetupDialog();
            return;
        }
        syncToServerInternal();
    }
    
    private void syncToServerInternal() {
        showProgress(true);
        syncManager.syncWatchHistoryToServer(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        updateSyncStatus();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void syncFromServer() {
        if (!syncManager.isSyncConfigured()) {
            showSyncSetupDialog();
            return;
        }
        syncFromServerInternal();
    }
    
    private void syncFromServerInternal() {
        showProgress(true);
        syncManager.syncWatchHistoryFromServer(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        loadWatchHistory();
                        updateSyncStatus();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void clearServerData() {
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa tất cả dữ liệu lịch sử trên server?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    showProgress(true);
                    syncManager.clearServerWatchHistory(new WatchHistorySyncManager.SyncCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showProgress(false);
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                    updateSyncStatus();
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    showProgress(false);
                                    Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showClearAllConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = 
            new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Xác nhận xóa lịch sử")
                .setMessage("Bạn có chắc muốn xóa TOÀN BỘ lịch sử xem?\n\n⚠️ Thao tác này sẽ xóa:\n• Lịch sử xem trên thiết bị này\n• Lịch sử xem trên server (nếu có đồng bộ)\n\nKhông thể hoàn tác!")
                .setPositiveButton("XÓA TẤT CẢ", (dialog, which) -> {
                    clearAllWatchHistory();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void clearAllWatchHistory() {
        showProgress(true);
        syncManager.clearAllWatchHistory(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        loadWatchHistory(); // Refresh danh sách
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    /**
     * API-Only approach: Always load fresh data from server
     * Proper user switching support
     */
    private void loadWatchHistory() {
        android.util.Log.d("WatchHistory", "📋 loadWatchHistory() called");
        showProgress(true);
        
        // Pure API-only approach - always fetch from server
        String userEmail = syncManager.getCurrentUserEmail();
        android.util.Log.d("WatchHistory", "👤 Loading history for user: " + userEmail + ", tracked: " + currentUserEmail);
        
        // CRITICAL: Check for user switching before loading data
        boolean userSwitched = (currentUserEmail == null && userEmail != null) ||
                              (currentUserEmail != null && !currentUserEmail.equals(userEmail));
        
        if (userSwitched) {
            // User switched - force clear SharedPreferences cache immediately
            android.util.Log.d("WatchHistory", "🔄 User switched detected in loadWatchHistory: " + currentUserEmail + " → " + userEmail);
            forceClearSharedPreferencesCache();
        }
        if (userEmail == null) {
            // No login = no history access - clear current user tracking
            currentUserEmail = null;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    showProgress(false);
                    historyItems.clear();
                    adapter.updateItems(historyItems);
                    updateEmptyState();
                    Toast.makeText(getContext(), "⚠️ Vui lòng đăng nhập để xem lịch sử", Toast.LENGTH_SHORT).show();
                });
            }
            return;
        }
        
        // Update current user tracking
        currentUserEmail = userEmail;
        
        // CRITICAL: Clear any cached data first to prevent showing wrong user's data
        historyItems.clear();
        adapter.updateItems(historyItems);
        
        // Force clear local cache before syncing from server
        syncManager.clearLocalWatchHistory(new WatchHistorySyncManager.SyncCallback() {
            @Override
            public void onSuccess(String clearMessage) {
                // After clearing cache, fetch fresh data from server for current user
                syncManager.syncWatchHistoryFromServer(new WatchHistorySyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Get fresh data for current user only
                        List<WatchHistorySyncItem.WatchHistoryItem> items = syncManager.getWatchHistoryForDisplay();
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showProgress(false);
                                historyItems.clear();
                                historyItems.addAll(items);
                                adapter.updateItems(historyItems);
                                updateEmptyState();
                            });
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showProgress(false);
                                historyItems.clear();
                                adapter.updateItems(historyItems);
                                updateEmptyState();
                                Toast.makeText(getContext(), "❌ Lỗi tải lịch sử: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            }
            
            @Override
            public void onError(String clearError) {
                // If cache clear fails, still try to sync from server
                syncManager.syncWatchHistoryFromServer(new WatchHistorySyncManager.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        List<WatchHistorySyncItem.WatchHistoryItem> items = syncManager.getWatchHistoryForDisplay();
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showProgress(false);
                                historyItems.clear();
                                historyItems.addAll(items);
                                adapter.updateItems(historyItems);
                                updateEmptyState();
                            });
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showProgress(false);
                                historyItems.clear();
                                adapter.updateItems(historyItems);
                                updateEmptyState();
                                Toast.makeText(getContext(), "❌ Lỗi tải lịch sử: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            }
        });
    }

    private List<WatchHistorySyncItem.WatchHistoryItem> generateSampleData() {
        List<WatchHistorySyncItem.WatchHistoryItem> sampleData = new ArrayList<>();
        
        WatchHistorySyncItem.WatchHistoryItem item1 = new WatchHistorySyncItem.WatchHistoryItem();
        item1.setTitle("Phim Hành Động 1");
        item1.setPosterUrl("https://example.com/poster1.jpg");
        item1.setCreatedAt(System.currentTimeMillis() - 86400000); // 1 day ago
        item1.setPosition(3600);
        item1.setDuration(7200);
        sampleData.add(item1);
        
        WatchHistorySyncItem.WatchHistoryItem item2 = new WatchHistorySyncItem.WatchHistoryItem();
        item2.setTitle("Phim Tình Cảm 2");
        item2.setPosterUrl("https://example.com/poster2.jpg");
        item2.setCreatedAt(System.currentTimeMillis() - 172800000); // 2 days ago
        item2.setPosition(2400);
        item2.setDuration(5400);
        sampleData.add(item2);
        
        return sampleData;
    }

    private void updateEmptyState() {
        if (historyItems.isEmpty()) {
            historyGridView.setVisibility(View.GONE);
            tvEmptyMessage.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("Chưa có lịch sử xem nào");
        } else {
            historyGridView.setVisibility(View.VISIBLE);
            tvEmptyMessage.setVisibility(View.GONE);
        }
    }

    private void updateSyncStatus() {
        String userEmail = syncManager.getCurrentUserEmail();
        if (userEmail != null) {
            tvSyncStatus.setText("🟢 API-Only Mode: " + userEmail);
            tvSyncStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvSyncStatus.setText("🔴 Chưa đăng nhập - Không thể truy cập lịch sử");
            tvSyncStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        
        if (btnSync != null) btnSync.setEnabled(!show);
        if (btnRefresh != null) btnRefresh.setEnabled(!show);
        if (btnClearAll != null) btnClearAll.setEnabled(!show);
    }
    
    /**
     * Public method to force clear all cached data when user logs out
     * Call this from logout activity to ensure clean state
     */
    public void onUserLogout() {
        currentUserEmail = null;
        
        // CRITICAL: Force clear SharedPreferences cache on logout
        forceClearSharedPreferencesCache();
        
        historyItems.clear();
        if (adapter != null) {
            adapter.updateItems(historyItems);
        }
        updateSyncStatus();
        updateEmptyState();
    }
    
    /**
     * Public method to refresh data when user logs in
     * Call this from login activity to ensure fresh data
     */
    public void onUserLogin() {
        String newUserEmail = syncManager.getCurrentUserEmail();
        if (newUserEmail != null && !newUserEmail.equals(currentUserEmail)) {
            currentUserEmail = newUserEmail;
            
            // CRITICAL: Force clear SharedPreferences cache for user switching
            forceClearSharedPreferencesCache();
            
            historyItems.clear();
            if (adapter != null) {
                adapter.updateItems(historyItems);
            }
            updateSyncStatus();
            loadWatchHistory(); // Load fresh data for new user
        }
    }
    
    /**
     * Static method to clear watch history cache from anywhere in the app
     * Call this from logout activities to ensure proper user switching
     */
    public static void clearWatchHistoryCache(android.content.Context context) {
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("watch_history_sync", 0);
            android.util.Log.d("WatchHistory", "🧹 Static clearing SharedPreferences cache on logout");
            
            prefs.edit()
                .remove("local_watch_history")  // PREF_LOCAL_HISTORY key
                .remove("sync_user_id")         // PREF_SYNC_USER_ID key
                .remove("sync_email")           // PREF_SYNC_EMAIL key  
                .remove("last_sync_time")       // PREF_LAST_SYNC_TIME key
                .apply();
                
            android.util.Log.d("WatchHistory", "✅ Static SharedPreferences cache cleared successfully");
        } catch (Exception e) {
            android.util.Log.e("WatchHistory", "❌ Error clearing watch history cache: " + e.getMessage());
        }
    }

    /**
     * Force clear SharedPreferences cache to prevent user data mixing
     * This is critical for proper user switching
     */
    private void forceClearSharedPreferencesCache() {
        try {
            // Direct access to SharedPreferences to clear watch history cache
            // Use the same SharedPreferences name as WatchHistorySyncManager
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("watch_history_sync", 0);
            android.util.Log.d("WatchHistory", "🧹 Force clearing SharedPreferences cache: watch_history_sync");
            
            prefs.edit()
                .remove("local_watch_history")  // PREF_LOCAL_HISTORY key
                .remove("sync_user_id")         // PREF_SYNC_USER_ID key
                .remove("sync_email")           // PREF_SYNC_EMAIL key  
                .remove("last_sync_time")       // PREF_LAST_SYNC_TIME key
                .apply();
                
            android.util.Log.d("WatchHistory", "✅ SharedPreferences cache cleared successfully");
            Toast.makeText(getContext(), "🔄 Đã xóa cache cũ cho user mới", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Fallback - just clear via syncManager
            syncManager.clearLocalWatchHistory(new WatchHistorySyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {}
                @Override  
                public void onError(String error) {}
            });
        }
    }
}