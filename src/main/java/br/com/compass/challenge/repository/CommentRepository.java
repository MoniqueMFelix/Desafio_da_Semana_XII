package br.com.compass.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.compass.challenge.entity.Comment;
import br.com.compass.challenge.entity.Post;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    void deleteByPost(Post post);

}
