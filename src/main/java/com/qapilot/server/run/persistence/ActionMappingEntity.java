package com.qapilot.server.run.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * action_mappings 테이블 — TC 별 ActionMapping (steps + api_endpoint). 읽기 전용 매핑.
 *
 * <p>service_id + tc_id 로 version DESC 최신 1건을 조회한다.
 *
 * <p>Author: C
 * <br>Created: 2026-06-07
 */
@Entity
@Table(name = "action_mappings")
@Getter
@Setter
@NoArgsConstructor
public class ActionMappingEntity {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "tc_id", nullable = false)
    private String tcId;

    @Column(nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;
}
