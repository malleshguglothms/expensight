package com.gm.expensight.repository;

import com.gm.expensight.domain.model.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, UUID> {
}
