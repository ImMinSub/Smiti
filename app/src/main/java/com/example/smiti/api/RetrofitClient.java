package com.example.smiti.api;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    private static final String BASE_URL = "http://202.31.246.51:80/";
    private static RetrofitClient instance;
    private final ApiService apiService;
    private static Retrofit retrofit;

    private RetrofitClient() {
        // HTTP 로깅 인터셉터 추가 (상세 로그)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
            Log.d(TAG, "API 통신: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Log.d(TAG, "RetrofitClient 초기화 - 서버 URL: " + BASE_URL);

        // 재시도 인터셉터 생성
        Interceptor retryInterceptor = chain -> {
            Request request = chain.request();
            Response response = null;
            IOException ioException = null;
            int maxRetries = 3;
            
            for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
                try {
                    if (retryCount > 0) {
                        Log.d(TAG, "RetrofitClient - 재시도 #" + retryCount);
                    }
                    
                    if (response != null) {
                        response.close();
                    }
                    
                    response = chain.proceed(request);
                    
                    // 성공적인 응답이면 반환
                    if (response.isSuccessful()) {
                        return response;
                    } else if (response.code() >= 500) {
                        // 서버 오류인 경우만 재시도
                        response.close();
                        
                        // 재시도 전에 잠시 대기
                        int sleepTime = 1000 * (1 << retryCount); // 지수 백오프: 1s, 2s, 4s...
                        Thread.sleep(sleepTime);
                    } else {
                        // 서버 오류가 아니면 그대로 반환
                        return response;
                    }
                } catch (IOException e) {
                    ioException = e;
                    Log.e(TAG, "RetrofitClient - 요청 실패, 재시도 #" + (retryCount + 1) + ": " + e.getMessage());
                    
                    // 특정 네트워크 오류에 대해서만 재시도
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("unexpected end of stream") ||
                         e.getMessage().contains("timeout") ||
                         e.getMessage().contains("connection"))) {
                        
                        // 재시도 전에 잠시 대기
                        try {
                            int sleepTime = 1000 * (1 << retryCount); // 지수 백오프: 1s, 2s, 4s...
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("요청이 중단되었습니다", ie);
                        }
                    } else {
                        // 다른 종류의 오류는 바로 예외 발생
                        throw e;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("요청이 중단되었습니다", e);
                }
            }
            
            // 모든 재시도 실패 후
            if (ioException != null) {
                throw ioException;
            }
            
            // 이 코드에 도달하지 않아야 함
            throw new IOException("알 수 없는 오류로 요청 실패");
        };

        // OkHttpClient 구성 - 타임아웃 시간 증가 및 재시도 로직 추가
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)     // 연결 타임아웃 60초로 증가
                .readTimeout(60, TimeUnit.SECONDS)        // 읽기 타임아웃 60초로 증가
                .writeTimeout(60, TimeUnit.SECONDS)       // 쓰기 타임아웃 60초로 증가
                .addInterceptor(loggingInterceptor)       // 로깅 인터셉터 추가
                .addInterceptor(retryInterceptor)         // 재시도 인터셉터 추가
                .retryOnConnectionFailure(true)           // 연결 실패 시 재시도
                .build();

        try {
            // GSON 설정 - 필드 이름 그대로 사용
            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                    .disableHtmlEscaping()  // HTML 이스케이프 비활성화
                    .setPrettyPrinting()    // 가독성 좋게 JSON 출력
                    .serializeNulls()       // null 값도 직렬화
                    .create();
                    
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)  // OkHttpClient 설정
                    .addConverterFactory(GsonConverterFactory.create(gson))  // 커스텀 Gson 사용
                    .build();
            apiService = retrofit.create(ApiService.class);
            
            Log.d(TAG, "RetrofitClient 초기화 완료");
        } catch (Exception e) {
            Log.e(TAG, "RetrofitClient 초기화 오류: " + e.getMessage(), e);
            throw new RuntimeException("API 서비스 초기화 실패", e);
        }
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            try {
                instance = new RetrofitClient();
            } catch (Exception e) {
                Log.e(TAG, "RetrofitClient 인스턴스 생성 실패: " + e.getMessage(), e);
                // 실패 시 null 반환 대신 예외 전파
                throw new RuntimeException("RetrofitClient 인스턴스 생성 실패", e);
            }
        }
        return instance;
    }

    // 서버 연결 상태 확인 메서드 추가
    public static boolean isServerReachable() {
        try {
            // 간단한 API 호출로 서버 연결 확인
            ApiService service = getInstance().apiService;
            service.getAllGroups().execute();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "서버 연결 확인 실패: " + e.getMessage(), e);
            return false;
        }
    }

    // 인스턴스 초기화 (앱 시작 시 호출하면 좋음)
    public static void initialize() {
        getInstance();
    }

    // Retrofit 객체 초기화 메서드 추가
    private static void initializeRetrofit() {
        try {
            // HTTP 로깅 인터셉터 추가 (상세 로그)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
                Log.d(TAG, "API 통신: " + message));
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            Log.d(TAG, "RetrofitClient 초기화 - 서버 URL: " + BASE_URL);

            // 재시도 인터셉터 생성
            Interceptor retryInterceptor = chain -> {
                Request request = chain.request();
                Response response = null;
                IOException ioException = null;
                int maxRetries = 3;
                
                for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
                    try {
                        if (retryCount > 0) {
                            Log.d(TAG, "RetrofitClient - 재시도 #" + retryCount);
                        }
                        
                        if (response != null) {
                            response.close();
                        }
                        
                        response = chain.proceed(request);
                        
                        // 성공적인 응답이면 반환
                        if (response.isSuccessful()) {
                            return response;
                        } else if (response.code() >= 500) {
                            // 서버 오류인 경우만 재시도
                            response.close();
                            
                            // 재시도 전에 잠시 대기
                            int sleepTime = 1000 * (1 << retryCount); // 지수 백오프: 1s, 2s, 4s...
                            Thread.sleep(sleepTime);
                        } else {
                            // 서버 오류가 아니면 그대로 반환
                            return response;
                        }
                    } catch (IOException e) {
                        ioException = e;
                        Log.e(TAG, "RetrofitClient - 요청 실패, 재시도 #" + (retryCount + 1) + ": " + e.getMessage());
                        
                        // 특정 네트워크 오류에 대해서만 재시도
                        if (e.getMessage() != null && 
                            (e.getMessage().contains("unexpected end of stream") ||
                             e.getMessage().contains("timeout") ||
                             e.getMessage().contains("connection"))) {
                            
                            // 재시도 전에 잠시 대기
                            try {
                                int sleepTime = 1000 * (1 << retryCount); // 지수 백오프: 1s, 2s, 4s...
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new IOException("요청이 중단되었습니다", ie);
                            }
                        } else {
                            // 다른 종류의 오류는 바로 예외 발생
                            throw e;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("요청이 중단되었습니다", e);
                    }
                }
                
                // 모든 재시도 실패 후
                if (ioException != null) {
                    throw ioException;
                }
                
                // 이 코드에 도달하지 않아야 함
                throw new IOException("알 수 없는 오류로 요청 실패");
            };

            // OkHttpClient 구성 - 타임아웃 시간 조정 및 버퍼 크기 증가
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)  // 연결 타임아웃 60초로 증가
                    .readTimeout(60, TimeUnit.SECONDS)     // 읽기 타임아웃 60초로 증가
                    .writeTimeout(60, TimeUnit.SECONDS)    // 쓰기 타임아웃 60초로 증가
                    .addInterceptor(loggingInterceptor)    // 로깅 인터셉터 추가
                    .addInterceptor(retryInterceptor)      // 재시도 인터셉터 추가
                    .retryOnConnectionFailure(true)        // 연결 실패 시 재시도
                    .build();

            // GSON 설정 - 필드 이름 그대로 사용
            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                    .disableHtmlEscaping()  // HTML 이스케이프 비활성화
                    .setPrettyPrinting()    // 가독성 좋게 JSON 출력
                    .serializeNulls()       // null 값도 직렬화
                    .create();
                    
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)  // OkHttpClient 설정
                    .addConverterFactory(GsonConverterFactory.create(gson))  // 커스텀 Gson 사용
                    .build();
                    
            Log.d(TAG, "Retrofit 초기화 완료");
        } catch (Exception e) {
            Log.e(TAG, "Retrofit 초기화 오류: " + e.getMessage(), e);
            throw new RuntimeException("Retrofit 초기화 실패", e);
        }
    }

    public static ApiService getApiService() {
        return getInstance().apiService;
    }

    // 커스텀 OkHttpClient를 사용한 ApiService 인스턴스 생성
    public static ApiService getCustomApiService(OkHttpClient customClient) {
        if (retrofit == null) {
            initializeRetrofit();
        }
        
        // 로깅 인터셉터 추가 유지
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
            android.util.Log.d("RetrofitClient", "API 통신: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // 기존 인터셉터를 유지하면서 커스텀 클라이언트의 설정 적용
        OkHttpClient.Builder clientBuilder = customClient.newBuilder();
        clientBuilder.addInterceptor(loggingInterceptor);
        
        // 커스텀 클라이언트로 Retrofit 재구성
        Retrofit customRetrofit = retrofit.newBuilder()
                .client(clientBuilder.build())
                .build();
                
        return customRetrofit.create(ApiService.class);
    }
    
    // 기본 타임아웃이 설정된 ApiService 생성하기
    public static ApiService getApiServiceWithTimeout() {
        // 재시도 인터셉터 생성
        Interceptor retryInterceptor = chain -> {
            Request request = chain.request();
            Response response = null;
            IOException ioException = null;
            int maxRetries = 3;
            
            for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
                try {
                    if (retryCount > 0) {
                        Log.d(TAG, "RetrofitClient - 재시도 #" + retryCount);
                    }
                    
                    if (response != null) {
                        response.close();
                    }
                    
                    response = chain.proceed(request);
                    
                    // 성공적인 응답이면 반환
                    if (response.isSuccessful()) {
                        return response;
                    } else if (response.code() >= 500) {
                        // 서버 오류인 경우만 재시도
                        response.close();
                        
                        // 재시도 전에 잠시 대기
                        int sleepTime = 1000 * (1 << retryCount); // 지수 백오프: 1s, 2s, 4s...
                        Thread.sleep(sleepTime);
                    } else {
                        // 서버 오류가 아니면 그대로 반환
                        return response;
                    }
                } catch (IOException e) {
                    ioException = e;
                    Log.e(TAG, "RetrofitClient - 요청 실패, 재시도 #" + (retryCount + 1) + ": " + e.getMessage());
                    
                    // 특정 네트워크 오류에 대해서만 재시도
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("unexpected end of stream") ||
                         e.getMessage().contains("timeout") ||
                         e.getMessage().contains("connection"))) {
                        
                        // 재시도 전에 잠시 대기
                        try {
                            int sleepTime = 1000 * (1 << retryCount); // 지수 백오프: 1s, 2s, 4s...
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("요청이 중단되었습니다", ie);
                        }
                    } else {
                        // 다른 종류의 오류는 바로 예외 발생
                        throw e;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("요청이 중단되었습니다", e);
                }
            }
            
            // 모든 재시도 실패 후
            if (ioException != null) {
                throw ioException;
            }
            
            // 이 코드에 도달하지 않아야 함
            throw new IOException("알 수 없는 오류로 요청 실패");
        };
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(retryInterceptor)
                .retryOnConnectionFailure(true)
                .build();
                
        return getCustomApiService(client);
    }
}