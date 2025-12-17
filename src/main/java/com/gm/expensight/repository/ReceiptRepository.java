package com.gm.expensight.repository;

import com.gm.expensight.domain.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
