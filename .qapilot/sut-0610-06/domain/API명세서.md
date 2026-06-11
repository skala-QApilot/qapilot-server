# NOVA Self-Care API 명세서

> NOVA 모바일 셀프케어 채널이 제공하는 REST API의 전체 명세. 본 문서는 셀프케어 프론트엔드 및 사내 연계 시스템이 참조하는 단일 진실 공급원이다.

---

## 문서 정보

| 항목 | 내용 |
|------|------|
| 문서명 | NOVA Self-Care API 명세서 |
| 문서번호 | API-NSC-001 |
| 버전 | v1.4 |
| 발행일 | 2026-05-04 |
| 소관 부서 | NOVA개발3팀 (셀프케어 파트) |
| 분류 | 사내 — 임직원 한정 |

### 개정 이력

| 버전 | 일자 | 주요 변경 내용 |
|------|------|----------------|
| v1.0 | 2026-04-30 | 인증·요금제·회선 도메인 8개 엔드포인트 정의 |
| v1.1 | 2026-05-04 | 회선당 활성 요금제 1건 정책 반영. 회선 생성·변경 응답 일원화 |
| v1.2 | 2026-05-04 | 요금제 즉시 변경(`PATCH change-plan`) 및 변경 예약(`PUT scheduled-change`) 엔드포인트 추가 |
| v1.3 | 2026-05-04 | 부가서비스 토글, 사용량 요약, 청구 개요, 공지사항, 회선 해지 엔드포인트 추가. 총 18개 |
| v1.4 | 2026-05-04 | 요금제 변경 시 신규 회선 발급 모델 명시. 결제일 산정을 회원 가입일 기준으로 변경. 부가서비스 토글에 매칭 검증 추가 |

---

## 1. 개요

### 1.1 Base URL

| 환경 | URL |
|------|-----|
| 개발 (로컬) | `http://localhost:8000` |
| 스테이징 | `https://stg-api.nova-selfcare.internal` |
| 운영 | `https://api.nova-selfcare.internal` |

### 1.2 통신 규약

- 모든 요청·응답 본문은 `application/json; charset=utf-8` 이다.
- 운영·스테이징 환경은 TLS 1.2 이상을 강제한다. 평문 HTTP 통신은 개발 환경에 한정한다.
- 요청 본문이 있는 메서드(`POST`, `PUT`, `PATCH`)는 `Content-Type: application/json` 헤더가 필수이다.

### 1.3 시간 및 통화 표기

| 항목 | 표기 |
|------|------|
| 타임스탬프 | ISO 8601, UTC 기준(예: `2026-05-04T01:02:35.725707Z`) |
| 날짜 | ISO 8601 date (예: `2026-05-15`) |
| 통화 | KRW. 가격·금액 필드는 문자열로 직렬화된 소수점 2자리 정밀도(`Numeric(10,2)`) — 예: `"55000.00"` |

### 1.4 식별자 규약

| 식별자 | 형식 | 위치 |
|--------|------|------|
| 고객 ID | 정수 | JWT subject(`sub`), 응답 본문 `id`, `customer_id` |
| 회선 ID | 정수 | 경로 변수 `{order_id}` |
| 요금제 ID | 정수 | 경로 변수 `{plan_id}`, 본문 `plan_id` |
| 부가서비스 ID | 정수 | 경로 변수 `{benefit_id}` |
| 트레이스 ID | UUID v4 | 요청·응답 헤더 `X-Trace-Id` |

---

## 2. 인증 및 권한 모델

### 2.1 인증 방식

본 API는 **JWT 기반 Bearer 토큰** 인증을 사용한다.

- 토큰 발급: `POST /api/auth/login`
- 알고리즘: HS256
- 만료: 발급 시점부터 **1440분(24시간)**
- 페이로드: `{ "sub": "<customer_id>", "exp": <unix_ts> }`
- 서명 검증 비밀키는 환경변수(`JWT_SECRET`)로 주입한다. 운영 환경은 임의 32바이트 이상의 시크릿을 강제한다.

### 2.2 인증 헤더

```http
Authorization: Bearer <jwt_token>
```

`Authorization` 헤더 누락, `Bearer ` 접두사 누락, 토큰 위조·만료, 페이로드의 고객 미존재 시 모두 `401 Unauthorized`로 응답한다.

### 2.3 인가 모델

본 API는 **본인 자원 단일 권한 모델**을 채택한다.

- 인증된 모든 요청은 서버에서 `customer_id == JWT.sub` 를 강제한다.
- 타 고객의 회선·예약·부가서비스 자원에 대한 요청은 **존재 여부를 누설하지 않기 위해 일관되게 `404 Not Found`** 로 응답한다.
- 관리자·상담사 권한은 본 채널의 범위 밖이며 별도 운영 콘솔에서 제공한다.

---

## 3. 공통 응답 규약

### 3.1 응답 헤더

모든 응답에 다음 헤더가 포함된다.

| 헤더 | 값 | 설명 |
|------|-----|------|
| `Content-Type` | `application/json` | 응답 본문 형식 |
| `X-Trace-Id` | UUID v4 | 요청 추적용. 클라이언트가 헤더로 보내면 그대로 전파, 미수신 시 신규 발급 |

### 3.2 성공 응답

| 상태 코드 | 의미 | 사용 |
|-----------|------|------|
| `200 OK` | 정상 처리 | 조회·갱신 |
| `201 Created` | 자원 생성 | 회원가입, 회선 생성 |

### 3.3 오류 응답

오류 응답 본문은 다음의 두 가지 형식 중 하나를 따른다.

**비즈니스 오류** (4xx 일반)

```json
{
  "detail": "이미 구독 중인 요금제가 있습니다. 요금제 변경을 이용하세요."
}
```

**입력 검증 오류** (`422 Unprocessable Entity`)

```json
{
  "detail": [
    {
      "type": "value_error",
      "loc": ["body", "email"],
      "msg": "value is not a valid email address",
      "input": "invalid-email"
    }
  ]
}
```

### 3.4 표준 상태 코드

| 코드 | 사용처 |
|------|--------|
| `400 Bad Request` | 비즈니스 규칙 위반 (비활성/미존재 요금제, 예약일 과거, 비활성 회선의 부가서비스 변경 등) |
| `401 Unauthorized` | 인증 실패 (토큰 누락·위조·만료) |
| `404 Not Found` | 자원 미존재 또는 본인 외 자원 접근 |
| `409 Conflict` | 중복(이메일·동일 요금제) 또는 정책 위반(활성 요금제 보유 중 신규 가입) |
| `422 Unprocessable Entity` | 입력 형식 검증 실패 |

---

## 4. 인증 도메인 API

### 4.1 회원가입

```
POST /api/auth/signup
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |
| 설명 | 신규 회원을 등록한다 |

**요청 본문**

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| `email` | string | ✓ | 이메일 형식, 3~255자 |
| `password` | string | ✓ | 8~100자 |
| `name` | string | ✓ | 1~100자 |

**응답 — `201 Created`**

```json
{
  "id": 3,
  "email": "user@example.com",
  "name": "홍길동",
  "created_at": "2026-05-04T01:02:35.725707Z"
}
```

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `409` | 이메일 중복 | `Email already registered` |
| `422` | 형식 검증 실패 | (검증 상세) |

**예시**

```bash
curl -X POST http://localhost:8000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Passw0rd!","name":"홍길동"}'
```

---

### 4.2 로그인

```
POST /api/auth/login
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |
| 설명 | 자격증명을 검증하고 JWT를 발급한다 |

**요청 본문**

| 필드 | 타입 | 필수 |
|------|------|------|
| `email` | string | ✓ |
| `password` | string | ✓ |

**응답 — `200 OK`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "bearer",
  "expires_at": "2026-05-05T01:02:35.725707Z",
  "customer": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "created_at": "2026-05-04T01:02:35.725707Z"
  }
}
```

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `401` | 자격증명 불일치 | `Invalid credentials` |

---

### 4.3 본인 프로필 조회

```
GET /api/auth/me
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 인증된 본인의 프로필을 반환한다 |

**응답 — `200 OK`**

```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "created_at": "2026-05-04T01:02:35.725707Z"
}
```

---

## 5. 요금제 도메인 API

### 5.1 요금제 목록 조회

```
GET /api/plans
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |
| 설명 | 활성 상태(`is_active=true`)의 요금제를 ID 오름차순으로 반환한다 |

**응답 — `200 OK`**

```json
[
  {
    "id": 1,
    "code": "5G_LITE",
    "name": "5G Lite 5GB",
    "price": "33000.00",
    "data_gb": 5,
    "is_active": true
  },
  {
    "id": 2,
    "code": "5G_STANDARD",
    "name": "5G Standard 30GB",
    "price": "55000.00",
    "data_gb": 30,
    "is_active": true
  }
]
```

> `data_gb` 가 999 이상인 경우 화면에서 "무제한"으로 표시한다.

---

### 5.2 요금제 상세 조회

```
GET /api/plans/{plan_id}
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |
| 설명 | 단일 요금제의 상세를 반환한다 |

**경로 변수**

| 변수 | 타입 | 설명 |
|------|------|------|
| `plan_id` | integer | 요금제 ID |

**응답 — `200 OK`** : §5.1 항목과 동일한 객체

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `404` | 미존재 | `Plan not found` |

---

## 6. 회선 도메인 API

본 도메인의 모든 인증 엔드포인트는 호출 시작 시점에 회원의 **변경 예약 자동 적용**을 수행한다(§9 참조).

### 6.1 신규 가입

```
POST /api/orders
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 신규 회선을 생성한다. 회선당 활성 요금제 1건 정책에 따라 활성 회선 보유 회원의 호출은 거부된다 |

**요청 본문**

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| `items` | array | ✓ | 정확히 1개 |
| `items[].plan_id` | integer | ✓ | 활성 요금제 ID |
| `items[].qty` | integer | - | 기본값 1, 1로 고정 |

```json
{
  "items": [{ "plan_id": 2, "qty": 1 }]
}
```

**응답 — `201 Created`**

```json
{
  "id": 1,
  "customer_id": 1,
  "status": "CONFIRMED",
  "total_amount": "55000.00",
  "created_at": "2026-05-04T01:02:35.725707Z",
  "items": [
    {
      "id": 1,
      "plan_id": 2,
      "qty": 1,
      "plan": {
        "id": 2,
        "code": "5G_STANDARD",
        "name": "5G Standard 30GB",
        "price": "55000.00",
        "data_gb": 30,
        "is_active": true
      }
    }
  ],
  "scheduled_change": null
}
```

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `400` | 비활성/미존재 요금제 | `Inactive or missing plan_id: [...]` |
| `409` | 활성 회선 보유 중 | `이미 구독 중인 요금제가 있습니다. 요금제 변경을 이용하세요.` |

---

### 6.2 본인 회선 목록 조회

```
GET /api/orders
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 본인의 모든 회선을 ID 내림차순(최신순)으로 반환한다. 활성 회선(`CONFIRMED`)과 변경·해지로 인한 비활성 회선(`CANCELLED`)이 모두 포함된다 |

**응답 — `200 OK`** : §6.1 응답 객체의 배열

**Empty 응답**: 보유 회선이 없는 경우 빈 배열 `[]` 반환.

**비고**: 요금제 변경(§6.5) 또는 변경 예약 자동 적용(§10) 시 새 회선이 발급되고 기존 회선은 `CANCELLED`로 전환된다. 따라서 변경 이력을 가진 회원의 응답에는 동일 시점에 1건의 `CONFIRMED` + N건의 `CANCELLED`가 함께 포함된다. 클라이언트는 활성 회선을 `status === "CONFIRMED"` 필터로 식별한다.

---

### 6.3 회선 상세 조회

```
GET /api/orders/{order_id}
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 본인의 단일 회선 상세를 반환한다 |

**경로 변수**

| 변수 | 타입 |
|------|------|
| `order_id` | integer |

**응답 — `200 OK`** : §6.1 응답 객체

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `404` | 미존재 또는 본인 외 자원 | `Order not found` |

---

### 6.4 회선 해지

```
PATCH /api/orders/{order_id}/cancel
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 회선 상태를 `CANCELLED`로 전환한다. 활성 변경 예약이 있으면 동시 삭제한다 |

**응답 — `200 OK`** : §6.1 응답 객체 (`status: "CANCELLED"`, `scheduled_change: null`)

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `400` | 이미 해지된 회선 | `Order is already cancelled` |
| `404` | 미존재 또는 본인 외 자원 | `Order not found` |

---

### 6.5 요금제 즉시 변경

```
PATCH /api/orders/{order_id}/change-plan
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 활성 회선의 요금제를 즉시 변경한다. 본 요청은 **기존 회선을 `CANCELLED` 상태로 전환하고 신규 요금제로 신규 회선을 `CONFIRMED` 상태로 발급**한다. 활성 변경 예약은 동시 삭제되며 신규 회선의 부가서비스 선택은 OFF 상태로 시작한다 |

**요청 본문**

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| `plan_id` | integer | ✓ | 활성 요금제 ID |
| `qty` | integer | - | 기본값 1, 1로 고정 |

```json
{ "plan_id": 3 }
```

**응답 — `200 OK`** : §6.1 응답 객체 (**신규 발급된 회선**)

**중요**: 응답 본문의 `id` 는 **신규 발급된 회선의 식별자**이며 경로 변수의 `{order_id}` 와 다르다. 클라이언트는 이후 회선 조회·해지·부가서비스 토글 시 응답에 포함된 신규 `id` 를 사용해야 한다. 기존 회선은 동일 시점에 `CANCELLED` 로 전환되며 `GET /api/orders` 응답에 이력으로 노출된다.

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `400` | 활성 회선이 아님 | `CONFIRMED 상태의 오더만 요금제를 변경할 수 있습니다` |
| `400` | 비활성/미존재 요금제 | `Inactive or missing plan_id: ...` |
| `409` | 동일 요금제로의 변경 | `이미 이용 중인 요금제입니다` |
| `404` | 미존재 또는 본인 외 자원 | `Order not found` |

---

### 6.6 요금제 변경 예약

```
PUT /api/orders/{order_id}/scheduled-change
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 활성 회선에 미래 적용일의 요금제 변경 예약을 등록한다. 회선당 활성 예약 1건 정책에 따라 기존 예약이 있으면 덮어쓴다 (upsert) |

**요청 본문**

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| `plan_id` | integer | ✓ | 활성 요금제 ID |
| `effective_date` | date | ✓ | 오늘 이후 (`> today`) |
| `qty` | integer | - | 기본값 1, 1로 고정 |

```json
{ "plan_id": 5, "effective_date": "2026-06-01" }
```

**응답 — `200 OK`**

```json
{
  "id": 1,
  "order_id": 1,
  "new_plan_id": 5,
  "qty": 1,
  "effective_date": "2026-06-01",
  "requested_at": "2026-05-04T01:02:35.725707Z",
  "new_plan": {
    "id": 5,
    "code": "LTE_FAMILY",
    "name": "LTE Family 50GB",
    "price": "66000.00",
    "data_gb": 50,
    "is_active": true
  }
}
```

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `400` | 활성 회선이 아님 | `CONFIRMED 상태의 오더만 변경 예약할 수 있습니다` |
| `400` | 적용일이 오늘 이하 | `예약 변경일은 오늘 이후여야 합니다` |
| `400` | 비활성/미존재 요금제 | `Inactive or missing plan_id: ...` |
| `409` | 동일 요금제 예약 | `이미 이용 중인 요금제입니다` |
| `404` | 미존재 또는 본인 외 자원 | `Order not found` |

---

## 7. 부가서비스 도메인 API

### 7.1 회선별 부가서비스 상태 조회

```
GET /api/orders/{order_id}/benefits
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 회선의 요금제에 매칭된 부가서비스 카탈로그와 각 항목의 ON/OFF 상태를 반환한다 |

**응답 — `200 OK`**

```json
[
  {
    "benefit_id": 3,
    "code": "AI_SECRETARY",
    "name": "AI 통화 비서",
    "description": "AI가 스팸 전화를 실시간 차단하고 통화 내용을 자동 요약합니다",
    "is_active": true
  },
  {
    "benefit_id": 4,
    "code": "DEVICE_INS",
    "name": "단말 파손 보험",
    "description": "디스플레이 파손·침수 사고 시 수리비 최대 30만 원을 지원합니다",
    "is_active": false
  }
]
```

**비고**

- 회선이 부가서비스를 한 번도 토글한 적이 없는 경우 모든 항목의 `is_active`는 `false`로 응답한다.
- 부가서비스 카탈로그 매칭은 회선의 현재 요금제(`order.items[0].plan_id`) 기준이다.

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `404` | 미존재 또는 본인 외 자원 | `Order not found` |

---

### 7.2 부가서비스 토글

```
POST /api/orders/{order_id}/benefits/{benefit_id}/toggle
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 활성 회선의 단일 부가서비스를 ON/OFF 토글한다. 토글은 즉시 반영된다 |

**경로 변수**

| 변수 | 타입 |
|------|------|
| `order_id` | integer |
| `benefit_id` | integer |

**응답 — `200 OK`**

```json
{
  "benefit_id": 3,
  "is_active": true
}
```

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `400` | 비활성 회선의 토글 시도 | `Cannot modify benefits of a non-confirmed order` |
| `400` | 회선의 현재 요금제 카탈로그와 미매칭 | `현재 요금제에서 제공되지 않는 부가서비스입니다` |
| `404` | 회선 미존재 또는 본인 외 자원 | `Order not found` |
| `404` | 부가서비스 미존재 | `Benefit not found` |

**비고**

- 첫 토글 시점에 부가서비스 선택 레코드가 생성되며, 이후 호출 시 `is_active`가 반전된다.
- 본 API는 `benefit_id`가 회선의 현재 요금제(`order.items[0].plan_id`) 카탈로그에 매칭되는지 검증한다. 매칭되지 않는 부가서비스에 대한 토글 요청은 `400`으로 거부된다. 따라서 다른 요금제의 부가서비스 ID로 임의 토글하더라도 데이터 무결성이 보장된다.

---

## 8. 사용량·청구·공지 도메인 API

### 8.1 사용량 요약

```
GET /api/usage/summary
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 활성 회선의 요금제 제공량을 기준으로 데이터·음성·문자 사용량과 잔여량을 청구 주기 단위로 산정해 반환한다 |

**응답 — `200 OK`** (활성 회선 보유)

```json
{
  "has_active_plan": true,
  "current_plan_name": "5G Standard 30GB",
  "billing_cycle_start": "2026-05-01",
  "billing_cycle_end": "2026-05-31",
  "data_total_gb": 30.0,
  "data_used_gb": 12.4,
  "data_remaining_gb": 17.6,
  "data_unlimited": false,
  "voice_total_min": 300,
  "voice_used_min": 124,
  "voice_remaining_min": 176,
  "voice_unlimited": false,
  "sms_total": 200,
  "sms_used": 38,
  "sms_remaining": 162,
  "sms_unlimited": false,
  "updated_at": "2026-05-04T01:02:35.725707Z"
}
```

**응답 — `200 OK`** (활성 회선 미보유)

```json
{
  "has_active_plan": false,
  "current_plan_name": null,
  "billing_cycle_start": "2026-05-01",
  "billing_cycle_end": "2026-05-31",
  "data_total_gb": null,
  "data_used_gb": null,
  "data_remaining_gb": null,
  "data_unlimited": false,
  "voice_total_min": null,
  "voice_used_min": null,
  "voice_remaining_min": null,
  "voice_unlimited": false,
  "sms_total": null,
  "sms_used": null,
  "sms_remaining": null,
  "sms_unlimited": false,
  "updated_at": "2026-05-04T01:02:35.725707Z"
}
```

**비고**

- 무제한 요금제(`data_gb >= 999`)의 경우 `data_unlimited: true`, `data_total_gb: null`, `data_remaining_gb: null` 로 응답하고 `data_used_gb`만 누적치를 표기한다. 음성·문자도 동일 규약을 따른다.
- 청구 주기는 매월 1일~말일 (KST).

---

### 8.2 청구 개요

```
GET /api/billing/overview
```

| 항목 | 내용 |
|------|------|
| 인증 | Bearer |
| 설명 | 본인의 청구 개요와 최근 4개월 청구 이력을 반환한다 |

**응답 — `200 OK`**

```json
{
  "has_active_plan": true,
  "current_due_amount": "55000.00",
  "next_payment_date": "2026-05-25",
  "payment_method_label": "신한카드 1234",
  "auto_pay_enabled": true,
  "estimated_next_amount": "66000.00",
  "invoices": [
    {
      "month_label": "2026.05",
      "amount": "55000.00",
      "due_date": "2026-05-25",
      "status": "납부 예정",
      "plan_name": "5G Standard 30GB"
    },
    {
      "month_label": "2026.04",
      "amount": "55000.00",
      "due_date": "2026-04-25",
      "status": "납부 완료",
      "plan_name": "5G Standard 30GB"
    }
  ]
}
```

**비고**

- **결제일 산정**: `next_payment_date` 와 invoice 의 `due_date` 는 **회원의 가입일 일자(day)** 를 매월 결제일로 적용한다. 해당 월에 가입일과 동일한 일자가 없는 경우(예: 가입일 31일 + 2월) 그 달의 마지막 일자로 clip한다.
- **다음 결제일 결정**: 당월의 결제일이 오늘 이상이면 당월 결제일을, 그렇지 않으면 다음 달의 결제일을 `next_payment_date` 로 응답한다.
- **invoices 산정 범위**: `next_payment_date` 가 속한 결제월부터 직전 3개의 결제월까지 최대 4건. 단, 가입일 이전의 결제월은 노출되지 않으므로 가입한 지 4개월 미만인 회원의 invoices length는 4 미만일 수 있다.
- **invoice 금액**: 모든 invoice 의 `amount` 는 현재 활성 요금제의 월 요금을 일관되게 적용한다. 변경 이력으로 인한 과거 요금제 차이는 본 응답에서 반영되지 않는다(과거 청구 명세는 별도 시스템 연동 범위).
- 첫 invoice 는 다가오는 결제 예정월의 `status: "납부 예정"`, 나머지는 `"납부 완료"` 로 표기된다.
- 활성 변경 예약이 있는 경우 `estimated_next_amount` 에 신규 요금제 가격이 표시된다.
- 활성 회선이 없는 경우 `has_active_plan: false`, `current_due_amount: "0"`, `next_payment_date: null`, `invoices: []`.

---

### 8.3 공지사항 목록

```
GET /api/notices
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |
| 설명 | 공지를 고정 → 게시일 내림차순 → ID 오름차순으로 반환한다 |

**응답 — `200 OK`**

```json
[
  {
    "id": 1,
    "category": "서비스 점검",
    "title": "5월 정기 시스템 점검 안내",
    "summary": "2026년 5월 12일 02:00~04:00 사이 일부 조회 서비스가 일시 중단됩니다.",
    "body": "안정적인 서비스 제공을 위해 ...",
    "is_pinned": true,
    "published_at": "2026-05-04"
  }
]
```

**카테고리 값**: `서비스 점검`, `요금제`, `납부`, `이벤트`

---

### 8.4 공지사항 상세

```
GET /api/notices/{notice_id}
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |
| 설명 | 단일 공지의 본문을 반환한다 |

**응답 — `200 OK`**: §8.3 단일 객체

**오류**

| 코드 | 사유 | 메시지 |
|------|------|--------|
| `404` | 미존재 ID | `Notice not found` |

---

## 9. 운영 API

### 9.1 헬스체크

```
GET /health
```

| 항목 | 내용 |
|------|------|
| 인증 | 불필요 |
| 설명 | 백엔드 인스턴스의 기동 여부를 확인한다. 컨테이너 오케스트레이터의 라이브니스 프로브에 사용 |

**응답 — `200 OK`**

```json
{ "status": "ok" }
```

---

## 10. 변경 예약 자동 적용 동작

본 API는 별도의 배치 스케줄러를 두지 않으며, 회원의 인증된 요청 처리 시작 시점에 변경 예약을 자동 적용한다. 본 처리는 다음 엔드포인트에서 모두 수행된다.

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{order_id}`
- `PATCH /api/orders/{order_id}/cancel`
- `PATCH /api/orders/{order_id}/change-plan`
- `PUT /api/orders/{order_id}/scheduled-change`
- `GET /api/orders/{order_id}/benefits`
- `POST /api/orders/{order_id}/benefits/{benefit_id}/toggle`
- `GET /api/usage/summary`
- `GET /api/billing/overview`

자동 적용 처리 단계는 다음과 같다.

1. 회원의 변경 예약 중 `effective_date <= 오늘` 인 항목을 조회한다.
2. 신규 요금제가 활성이고 회선이 활성인 경우에 한해:
   - 기존 회선을 `CANCELLED` 상태로 전환한다.
   - 신규 요금제로 신규 회선을 `CONFIRMED` 상태로 발급한다(신규 `order_id` 부여).
   - 신규 회선의 부가서비스 선택은 비어 있는 상태로 시작한다(기존 회선의 부가서비스 선택은 승계되지 않는다).
3. 신규 요금제가 비활성이거나 회선이 해지된 경우 적용 없이 예약을 삭제한다.
4. 처리된 예약은 모두 삭제한다.

본 처리는 트랜잭션 내에서 수행되며, 처리 결과는 동일 요청의 응답 본문에 즉시 반영된다. 회원의 다음 `GET /api/orders` 응답에는 신규 발급된 `CONFIRMED` 회선과 직전 `CANCELLED` 회선이 함께 포함된다.

---

## 11. 데이터 모델 참조

### 11.1 Plan

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | integer | 시스템 식별자 |
| `code` | string | 사업자 정의 코드(예: `5G_STANDARD`). 시스템 전체 고유 |
| `name` | string | 표시명 |
| `price` | decimal | 월 요금 (KRW, 부가세 포함) |
| `data_gb` | integer? | 데이터 제공량(GB). 999 이상은 무제한 |
| `is_active` | boolean | 카탈로그 활성 여부 |

### 11.2 Order

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | integer | 회선 식별자. 요금제 변경 시점마다 신규 발급된다 |
| `customer_id` | integer | 회원 식별자 |
| `status` | string | `CONFIRMED` 또는 `CANCELLED`. 회원 의도 해지(§6.4)와 요금제 변경(§6.5) 또는 변경 예약 자동 적용(§10)으로 인한 자동 해지 모두 동일하게 `CANCELLED` 로 표기된다 |
| `total_amount` | decimal? | 월 요금 산정 결과 |
| `created_at` | datetime | 회선 생성 시각 (UTC) |
| `items` | array<OrderItem> | 회선의 라인 아이템(현 버전에서 항상 1건) |
| `scheduled_change` | ScheduledPlanChange? | 활성 변경 예약. 미보유 시 `null`. `CANCELLED` 회선의 경우 항상 `null` |

### 11.3 OrderItem

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | integer | 식별자 |
| `plan_id` | integer | 요금제 ID |
| `qty` | integer | 수량(1로 고정) |
| `plan` | Plan | 요금제 객체 |

### 11.4 ScheduledPlanChange

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | integer | 예약 식별자 |
| `order_id` | integer | 회선 식별자 |
| `new_plan_id` | integer | 변경 대상 요금제 ID |
| `qty` | integer | 수량(1로 고정) |
| `effective_date` | date | 적용일 |
| `requested_at` | datetime | 예약 등록 시각 (UTC) |
| `new_plan` | Plan | 변경 대상 요금제 객체 |

### 11.5 OrderBenefitState

| 필드 | 타입 | 설명 |
|------|------|------|
| `benefit_id` | integer | 부가서비스 식별자 |
| `code` | string | 사업자 정의 코드(예: `AI_SECRETARY`) |
| `name` | string | 표시명 |
| `description` | string | 설명 |
| `is_active` | boolean | 회선의 토글 ON/OFF 상태 |

### 11.6 UsageSummary

§8.1 응답 스키마 참조.

### 11.7 BillingOverview / BillingInvoice

§8.2 응답 스키마 참조.

### 11.8 Notice

§8.3 응답 스키마 참조.

---

## 12. 부록: 비즈니스 오류 메시지 인덱스

본 API가 응답하는 비즈니스 오류 메시지(`detail` 문자열)의 인덱스이다. 메시지는 한국어이며, 클라이언트는 메시지 자체를 사용자에게 노출해도 무방하다.

| 메시지 | HTTP | 발생 엔드포인트 |
|--------|------|-----------------|
| `Email already registered` | 409 | `POST /api/auth/signup` |
| `Invalid credentials` | 401 | `POST /api/auth/login` |
| `Missing bearer token` | 401 | 인증 필요 엔드포인트 전체 |
| `Invalid token` | 401 | 인증 필요 엔드포인트 전체 |
| `Customer not found` | 401 | 인증 필요 엔드포인트 전체 |
| `Plan not found` | 404 | `GET /api/plans/{id}` |
| `Inactive or missing plan_id: [...]` | 400 | `POST /api/orders`, `PATCH change-plan`, `PUT scheduled-change` |
| `이미 구독 중인 요금제가 있습니다. 요금제 변경을 이용하세요.` | 409 | `POST /api/orders` |
| `Order not found` | 404 | 회선 도메인 전체 |
| `Order is already cancelled` | 400 | `PATCH /api/orders/{id}/cancel` |
| `CONFIRMED 상태의 오더만 요금제를 변경할 수 있습니다` | 400 | `PATCH change-plan` |
| `CONFIRMED 상태의 오더만 변경 예약할 수 있습니다` | 400 | `PUT scheduled-change` |
| `이미 이용 중인 요금제입니다` | 409 | `PATCH change-plan`, `PUT scheduled-change` |
| `예약 변경일은 오늘 이후여야 합니다` | 400 | `PUT scheduled-change` |
| `Cannot modify benefits of a non-confirmed order` | 400 | `POST benefits/{id}/toggle` |
| `현재 요금제에서 제공되지 않는 부가서비스입니다` | 400 | `POST benefits/{id}/toggle` |
| `Benefit not found` | 404 | `POST benefits/{id}/toggle` |
| `Notice not found` | 404 | `GET /api/notices/{id}` |

---

## 부록 A. 참조 문서

| 문서 | 위치 | 관계 |
|------|------|------|
| 제품 요구사항 정의서 | `../prd/PRD.md` | 기능 요구사항·비즈니스 규칙 |
| 요금제·부가서비스 정책 | `../policy/요금제정책.md` | 요금제·부가서비스 카탈로그, 사용량 제공량 |
| 이용약관 | `../policy/이용약관.md` | 회원·사업자 권리·의무 |
| OpenAPI 자동 문서 | 운영 환경 `/docs` | Swagger UI (개발자 인터랙티브 테스트) |
