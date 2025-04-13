    package com.example.videoshortfirebase;

    import android.content.Context;
    import android.util.Log;

    import com.android.volley.Request;
    import com.android.volley.RequestQueue;
    import com.android.volley.toolbox.JsonObjectRequest;
    import com.android.volley.toolbox.Volley;

    import org.json.JSONException;
    import org.json.JSONObject;

    import java.util.HashMap;
    import java.util.Map;

    public class AuthService {
        static SupabaseConfig supabaseConfig;
        private static String SUPABASE_URL = SupabaseConfig.SUPABASE_URL;
        private static String API_KEY = SupabaseConfig.SUPABASE_API_KEY;
        private final Context context;
        private final RequestQueue queue;
        private SessionManager sessionManager;

        public AuthService(Context context) {
            this.context = context;
            this.queue = Volley.newRequestQueue(context);
        }
        public void signup(String email, String password, final Callback callback) {
            String signupUrl = SUPABASE_URL + "/auth/v1/signup";
            JSONObject body = new JSONObject();
            sessionManager = new SessionManager(context);
            try {
                body.put("email", email);
                body.put("password", password);
            } catch (JSONException e) {
                callback.onError("Lỗi tạo body: " + e.toString());
                return;
            }

            JsonObjectRequest signupRequest = new JsonObjectRequest(Request.Method.POST, signupUrl, body,
                    response -> {
                        Log.d("UserInfo", "Đăng ký xong");
                        // ✅ Sau khi đăng ký xong, gọi login luôn để lấy access_token
                        login(email, password, new Callback() {
                            @Override
                            public void onSuccess(JSONObject loginResponse) {
                                try {

                                    Log.d("UserInfo", "Vào đăng nhập nè");
                                    String accessToken = loginResponse.getString("access_token");
                                    Log.d("UserInfo", "Access token: " + accessToken);

                                     //Lấy thông tin user
                                    String userInfoUrl = SUPABASE_URL + "/auth/v1/user";
                                    JsonObjectRequest userInfoRequest = new JsonObjectRequest(Request.Method.GET, userInfoUrl, null,
                                            userResponse -> {
                                                try {
                                                    String userId = userResponse.getString("id");
                                                    String userEmail = userResponse.getString("email");
                                                    Log.d("UserInfo", "User info: " + userResponse.toString());

                                                    saveUserToCustomTable(userId, userEmail, callback);
                                                    sessionManager.saveAccessToken(accessToken);
                                                } catch (JSONException e) {
                                                    callback.onError("Lỗi đọc user info: " + e.toString());
                                                }
                                            },
                                            error -> {
                                                callback.onError("Không lấy được thông tin user: " + error.toString());
                                            }
                                    ) {
                                        @Override
                                        public Map<String, String> getHeaders() {
                                            Map<String, String> headers = new HashMap<>();
                                            headers.put("Authorization", "Bearer " + accessToken);
                                            headers.put("apikey", API_KEY);
                                            return headers;
                                        }
                                    };

                                    queue.add(userInfoRequest);
                                } catch (JSONException e) {
                                    callback.onError("Lỗi lấy access token sau đăng nhập: " + e.toString());
                                }
                            }

                            @Override
                            public void onError(String error) {
                                callback.onError("Đăng nhập sau đăng ký thất bại: " + error);
                            }
                        });
                    },
                    error -> {
                        String errorMsg = "Unknown error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        }
                        callback.onError("Signup thất bại: " + errorMsg);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            queue.add(signupRequest);
        }
//        public void signup(String email, String password, final Callback callback) {
//            String url = SUPABASE_URL + "/auth/v1/signup";
//            JSONObject body = new JSONObject();
//            try {
//                body.put("email", email);
//                body.put("password", password);
//            } catch (JSONException e) {
//                callback.onError(e.toString());
//                return;
//            }
//
//            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
//                callback::onSuccess,
//                error -> {
//                    String errorMsg = "Unknown error";
//                    if (error.networkResponse != null && error.networkResponse.data != null) {
//                        errorMsg = new String(error.networkResponse.data);
//                    }
//                    callback.onError(errorMsg);
//                }
//            ) {
//                @Override
//                public Map<String, String> getHeaders() {
//                    Map<String, String> headers = new HashMap<>();
//                    headers.put("apikey", API_KEY);
//                    headers.put("Authorization", "Bearer " + API_KEY);
//                    headers.put("Content-Type", "application/json");
//                    return headers;
//                }
//            };
//
//            queue.add(request);
//        }
        public void login(String email, String password, final Callback callback) {
            String url = SUPABASE_URL + "/auth/v1/token?grant_type=password";
            JSONObject body = new JSONObject();
            try {
                body.put("email", email);
                body.put("password", password);
            } catch (JSONException e) {
                callback.onError(e.toString());
                return;
            }
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    callback::onSuccess,
                    error -> {
                        String errorMsg = "Unknown error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        }
                        callback.onError(errorMsg);
                    }

            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", API_KEY);
                    headers.put("Authorization", "Bearer " + API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            queue.add(request);
        }
        private void saveUserToCustomTable(String userId, String userEmail, final Callback callback) {
            // Tạo URL cho yêu cầu insert dữ liệu vào bảng user_custom
            String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/users_custom";

            // Tạo body dữ liệu để insert vào bảng user_custom
            JSONObject body = new JSONObject();
            try {
                body.put("id", userId);
                body.put("email", userEmail);
                Log.d("AuthService", "Saving to user_custom: " + body.toString());
            } catch (JSONException e) {
                callback.onError("Error creating JSON body for user_custom: " + e.toString());
                return;
            }

            // Gửi yêu cầu insert vào bảng user_custom
            JsonObjectRequest insertRequest = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        // Xử lý thành công khi lưu vào bảng user_custom
                        callback.onSuccess(response);
                    },
                    error -> {
                        // Xử lý lỗi khi không thành công
                        String errorMsg = "Unknown error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        } else if (error.getMessage() != null) {
                            errorMsg = error.getMessage();
                        }
                        callback.onError(errorMsg);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("apikey", API_KEY); // Chỉ cần API key để truy cập REST API
                    headers.put("Authorization", "Bearer " + API_KEY); // Nếu cần Bearer token cho REST API
                    headers.put("Content-Type", "application/json");
                    headers.put("Prefer", "resolution=merge-duplicates");
                    return headers;
                }
            };

            // Thêm yêu cầu vào queue
            queue.add(insertRequest);
        }
        public interface Callback {
            void onSuccess(JSONObject response);
            void onError(String error);
        }
    }
