package br.com.compass.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.compass.challenge.entity.History;

public interface HistoryRepository extends JpaRepository<History, Long> {

}
