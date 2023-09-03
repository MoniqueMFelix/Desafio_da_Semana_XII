package br.com.compass.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.compass.challenge.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

}
