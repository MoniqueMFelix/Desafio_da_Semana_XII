package br.com.compass.challenge.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import br.com.compass.challenge.entity.Comment;
import br.com.compass.challenge.entity.Post;
import br.com.compass.challenge.entity.History;
import br.com.compass.challenge.entity.Status;
import br.com.compass.challenge.repository.CommentRepository;
import br.com.compass.challenge.repository.PostRepository;

@Service
public class PostService {

    private RestTemplate restTemplate;
    private PostRepository postRepository;
    private CommentRepository commentRepository;

    @Autowired
    public PostService(RestTemplate restTemplate, PostRepository postRepository, CommentRepository commentRepository) {
        this.restTemplate = restTemplate;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    public List<Post> findAll() {
        return postRepository.findAll();
    }

    public void disablePost(Long id) {
        if (id >= 1 && id <= 100) {
            Post post = postRepository.findById(id).get();
            History lastState = post.getHistory().get(post.getHistory().size() - 1);

            if (lastState.getStatus() == Status.ENABLED) {
                CompletableFuture.runAsync(() -> {
                    post.getHistory().add(new History(null, new Date(), Status.DISABLED, post));
                    postRepository.save(post);
                });
            } else {
                throw new RuntimeException("Status is not ENABLED");
            }
        } else {
            throw new RuntimeException("Range is not between [1,100]");
        }
    }

    public void processPost(Long id) {
        if (id >= 1 && id <= 100) {
            if (!postRepository.existsById(id)) {
                CompletableFuture.runAsync(() -> {
                    Post post = new Post(id, null, null, new ArrayList<>(), new ArrayList<>());
                    try {
                        post.getHistory().add(new History(null, new Date(), Status.CREATED, post));
                        post.getHistory().add(new History(null, new Date(), Status.POST_FIND, post));
                        Post postApi = fetchPostFromApi(post.getId());
                        post.setTitle(postApi.getTitle());
                        post.setBody(postApi.getBody());
                        post.getHistory().add(new History(null, new Date(), Status.POST_OK, post));
                        post.getHistory().add(new History(null, new Date(), Status.COMMENTS_FIND, post));
                        List<Comment> comments = fetchCommentsFromApi(id);
                        for (Comment comment : comments) {
                            post.getComments().add(comment);
                            comment.setPost(post);
                        }
                        post.getHistory().add(new History(null, new Date(), Status.COMMENTS_OK, post));
                        post.getHistory().add(new History(null, new Date(), Status.ENABLED, post));
                        postRepository.save(post);
                    } catch (Exception e) {
                        post.getHistory().add(new History(null, new Date(), Status.FAILED, post));
                        post.getHistory().add(new History(null, new Date(), Status.DISABLED, post));
                        postRepository.save(post);
                    }
                });
            } else {
                throw new RuntimeException("Post already exists");
            }
        } else {
            throw new RuntimeException("Range is not between [1,100]");
        }

    }

    @Transactional
    public void reprocessPost(Long id) {
        if (id >= 1 && id <= 100) {
            Post post = postRepository.findById(id).get();
            History lastState = post.getHistory().get(post.getHistory().size() - 1);
            if (lastState.getStatus() == Status.ENABLED
                    || lastState.getStatus() == Status.DISABLED) {

                try {
                    post.setBody(null);
                    post.setTitle(null);
                    post.getHistory().add(new History(null, new Date(), Status.UPDATING, post));
                    post.getHistory().add(new History(null, new Date(), Status.POST_FIND, post));
                    postRepository.save(post);
                    post.setComments(new ArrayList<>());
                    commentRepository.deleteByPost(post);

                    Post postApi = fetchPostFromApi(post.getId());
                    post.setTitle(postApi.getTitle());
                    post.setBody(postApi.getBody());
                    post.getHistory().add(new History(null, new Date(), Status.POST_OK, post));
                    post.getHistory().add(new History(null, new Date(), Status.COMMENTS_FIND, post));
                    List<Comment> comments = fetchCommentsFromApi(id);
                    for (Comment comment : comments) {
                        comment.setPost(post);
                        commentRepository.save(comment);
                    }

                    post.getHistory().add(new History(null, new Date(), Status.COMMENTS_OK, post));
                    post.getHistory().add(new History(null, new Date(), Status.ENABLED, post));
                    postRepository.save(post);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    post.getHistory().add(new History(null, new Date(), Status.FAILED, post));
                    post.getHistory().add(new History(null, new Date(), Status.DISABLED, post));
                    postRepository.save(post);
                }
            } else {
                throw new RuntimeException("Status is not ENABLED or DISABLED");
            }
        } else {
            throw new RuntimeException("Range is not between [1,100]");
        }
    }

    public Post fetchPostFromApi(Long id) {
        String url = "https://jsonplaceholder.typicode.com/posts/" + id;
        Post responsePost = restTemplate.getForObject(url, Post.class);
        return responsePost;
    }

    public List<Comment> fetchCommentsFromApi(Long id) {
        String url = "https://jsonplaceholder.typicode.com/posts/" + id + "/comments";
        ResponseEntity<Comment[]> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                Comment[].class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            Comment[] items = responseEntity.getBody();
            return Arrays.asList(items);
        } else {
            return Collections.emptyList();
        }
    }
}
