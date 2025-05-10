package com.example.smiti.api;
//Retrofit을 사용한 API 엔드포인트 정의 인터페이스스
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    // 그룹 상세조회 API
    @GET("groups/{groupId}")
    Call<ApiResponse> getGroupDetail(@Path("groupId") int groupId);

    // 그룹 검색 API - 표준 API 엔드포인트가 없으므로 기본 groups 엔드포인트를 사용
    // 클라이언트 측에서 검색어 필터링 수행
    @GET("groups")
    Call<ApiResponse> searchGroups(@Query("keyword") String keyword);

    @GET("groups/smbti-scores")
    Call<ApiResponse> getGroupsWithSmbtiScore(@Query("email") String email);

    @POST("groups/{groupId}/users")
    Call<ApiResponse> addUserToGroup(@Path("groupId") int groupId, @Body JoinGroupRequest request);

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
    // 파일 없는 게시글 생성 API - 명세서에 따라 multipart/form-data 형식 사용
    @Multipart
    @POST("posts")
    Call<ApiResponse> createPost(
        @Part("email") RequestBody email,
        @Part("board_type") RequestBody boardType, 
        @Part("title") RequestBody title,
        @Part("content") RequestBody content
    );
    
    // 파일 있는 게시글 생성 API
    @Multipart
    @POST("posts")
    Call<ApiResponse> createPostWithFile(
            @Part("email") RequestBody email,
            @Part("board_type") RequestBody boardType,
            @Part("title") RequestBody title,
            @Part("content") RequestBody content,
            @Part MultipartBody.Part file
    );

    @PUT("posts/{postId}")
    Call<ApiResponse> updatePost(
        @Path("postId") int postId, 
        @Body Map<String, String> request
    );

    @GET("posts")
    Call<ApiResponse> getPosts(
        @Query("page") int page,
        @Query("size") int size,
        @Query("board_type") String boardType
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

    @DELETE("posts/{postId}")
    Call<ApiResponse> deletePost(@Path("postId") int postId);

    @GET("board-uploads/{fileName}")
    Call<ResponseBody> downloadBoardFile(@Path("fileName") String fileName);

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

    // 업데이트된 API 명세에 따른 새 메서드 추가
    // 게시글 좋아요(추천)
    @POST("posts/{postId}/like")
    Call<ApiResponse> likePost(
        @Path("postId") int postId,
        @Body Map<String, String> request
    );

    // 게시글 싫어요(비추천)
    @POST("posts/{postId}/dislike")
    Call<ApiResponse> dislikePost(
        @Path("postId") int postId,
        @Body Map<String, String> request
    );

    // 게시글 댓글 작성 (새 API 경로)
    @POST("posts/{postId}/comments")
    Call<ApiResponse> addComment(
        @Path("postId") int postId,
        @Body Map<String, String> request
    );

    // 게시글 댓글 삭제 (새 API 경로)
    @DELETE("comments/{commentId}")
    Call<ApiResponse> deleteCommentById(
        @Path("commentId") int commentId,
        @Body Map<String, String> request
    );

    // 게시글 댓글 조회 (새 API 경로)
    @GET("comments/{postId}")
    Call<ApiResponse> getCommentsByPostId(@Path("postId") int postId);
}