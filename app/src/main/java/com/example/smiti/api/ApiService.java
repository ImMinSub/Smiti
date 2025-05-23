package com.example.smiti.api;

//Retrofit을 사용한 API 엔드포인트 정의 인터페이스스
import java.util.Map;

import okhttp3.MultipartBody; // 추가
import okhttp3.RequestBody;   // 추가
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
// import retrofit2.http.Headers; // 현재 사용되지 않음
import retrofit2.http.Multipart; // 추가
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;     // 추가
import retrofit2.http.Path;
import retrofit2.http.Query;
import androidx.annotation.Nullable; // @Nullable 사용 시 추가


public interface ApiService {
    // 사용자 관련 API
    @POST("users")
    Call<ApiResponse> registerUser(@Body AddUserRequest request);

    @POST("users/login")
    Call<ApiResponse> login(@Body LoginRequest request);

    @PUT("users/smbti")
    Call<ApiResponse> updateSmbti(@Body UpdateSmbtiRequest request);

    @GET("users/me")
    Call<ApiResponse> getMyInfo(@Query("email") String email);

    @GET("users/me/groups")
    Call<ApiResponse> getMyGroups(@Query("email") String email);

    // 그룹 관련 API
    @POST("groups/recommend")
    Call<ApiResponse> recommendGroup(@Body FindGroupRequest request);

    @GET("groups")
    Call<ApiResponse> getAllGroups();

    @GET("groups/{groupId}")
    Call<ApiResponse> getGroupDetail(@Path("groupId") int groupId);

    @GET("groups")
    Call<ApiResponse> searchGroups(@Query("keyword") String keyword);

    @GET("groups/smbti-scores")
    Call<ApiResponse> getGroupsWithSmbtiScore(@Query("email") String email);

    @POST("groups/{groupId}/users")
    Call<ApiResponse> addUserToGroup(@Path("groupId") int groupId, @Body JoinGroupRequest request);

    // 그룹 멤버 조회 API 추가
    @GET("groups/{groupId}/users")
    Call<ApiResponse> getGroupUsers(@Path("groupId") int groupId);

    @POST("groups")
    Call<ApiResponse> createGroup(@Body CreateGroupRequest request);

    // 스터디 시간 관련 API
    @PUT("users/me/available-times")
    Call<ApiResponse> updateAvailableTimes(@Body AvailableTimesRequest request);

    @GET("users/me/available-times")
    Call<ApiResponse> getAvailableTimes(@Query("email") String email);

    @GET("groups/{groupId}/like-times")
    Call<ApiResponse> getRecommendedStudyTimes(@Path("groupId") int groupId);

    // 게시판 관련 API
    @Multipart
    @POST("posts")
    Call<ApiResponse> createPost(
            @Part("email") RequestBody email,
            @Part("board_type") RequestBody boardType,
            @Part("title") RequestBody title,
            @Part("content") RequestBody content
    );

    @Multipart
    @POST("posts")
    Call<ApiResponse> createPostWithFile(
            @Part("email") RequestBody email,
            @Part("board_type") RequestBody boardType,
            @Part("title") RequestBody title,
            @Part("content") RequestBody content,
            @Part MultipartBody.Part file
    );

    // !!!!! 여기가 수정된 updatePost 메소드 !!!!!

    @PUT("posts/{postId}")
    Call<ApiResponse> updatePost(
            @Path("postId") int postId,         // URL 경로의 postId
            @Body Map<String, Object> request  // 요청 본문 (JSON 형태)
    );

    @GET("posts")
    Call<ApiResponse> getPosts(
            @Query("page") int page,
            @Query("size") int size,
            @Query("board_type") @Nullable String boardType
    );

    @GET("posts/search")
    Call<ApiResponse> searchPosts(
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size,
            @Query("sort") String sort
    );

    @GET("posts/{postId}")
    Call<ApiResponse> getPost(@Path("postId") int postId);

    @GET("posts/{postId}")
    Call<ApiResponse> getPost(@Path("postId") int postId, @Query("email") String email);

    @HTTP(method = "DELETE", path = "posts/{postId}", hasBody = true)
    Call<ApiResponse> deletePost(@Path("postId") int postId, @Body Map<String, Object> body);

    @GET("board_uploads/{fileName}")
    Call<ResponseBody> downloadBoardFile(@Path("fileName") String fileName);

    // ... (나머지 API 정의들은 이전과 동일하다고 가정) ...
    // 문서 기반 AI 질의응답 API
    @POST("ai/ask")
    Call<ApiResponse> askAI(@Body AiQuestionRequest request);

    // 실시간 채팅 API
    @GET("chat/{groupId}/history")
    Call<ApiResponse> getChatHistory(@Path("groupId") int groupId);

    @POST("chat/summary")
    Call<ApiResponse> getChatSummary(@Body GroupIdRequest request);

    @GET("uploads/{fileName}")
    Call<ResponseBody> downloadChatFile(@Path("fileName") String fileName);

    // 관리자 관련 API
    @POST("admins/promote")
    Call<ApiResponse> promoteAdmin(@Body AdminRequest request);

    @POST("admins/demote")
    Call<ApiResponse> demoteAdmin(@Body AdminRequest request);

    // 댓글 관련 API
    @POST("posts/{postId}/comments")
    Call<ApiResponse> createComment(
            @Path("postId") int postId,
            @Body Map<String, String> request
    );

    @GET("posts/{postId}/comments")
    Call<ApiResponse> getComments(@Path("postId") int postId);

    @PUT("posts/{postId}/comments/{commentId}")
    Call<ApiResponse> updateComment(
            @Path("postId") int postId,
            @Path("commentId") int commentId,
            @Body Map<String, String> request
    );

    @DELETE("posts/{postId}/comments/{commentId}")
    Call<ApiResponse> deleteComment(
            @Path("postId") int postId,
            @Path("commentId") int commentId
    );

    // 좋아요 관련 API
    @POST("posts/{postId}/likes")
    Call<ApiResponse> addLike(
            @Path("postId") int postId,
            @Body Map<String, String> request
    );

    @DELETE("posts/{postId}/likes")
    Call<ApiResponse> removeLike(
            @Path("postId") int postId,
            @Query("email") String email
    );

    @POST("posts/{postId}/like")
    Call<ApiResponse> likePost(
            @Path("postId") int postId,
            @Body Map<String, String> request
    );

    @POST("posts/{postId}/dislike")
    Call<ApiResponse> dislikePost(
            @Path("postId") int postId,
            @Body Map<String, String> request
    );

    // addComment는 createComment와 중복될 수 있으므로, 하나만 사용하거나 명확히 구분
    // @POST("posts/{postId}/comments")
    // Call<ApiResponse> addComment(
    //         @Path("postId") int postId,
    //         @Body Map<String, String> request
    // );

    @HTTP(method = "DELETE", path = "comments/{comment_id}", hasBody = true)
    Call<ApiResponse> deleteCommentById(
            @Path("comment_id") int commentId,
            @Body Map<String, String> request
    );

    @GET("comments/{postId}")
    Call<ApiResponse> getCommentsByPostId(@Path("postId") int postId);
}
