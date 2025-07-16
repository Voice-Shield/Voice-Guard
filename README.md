# Voice Guard
## 보이스 피싱 방지 어플
- Clova Speech API와 OpenAI의 Gpt API를 사용하여 수신된 통화를 분석하여 사용자에게 피싱 의심 전화 수신 시 경고 알림

### 지원 기능
- 전화 수신 시 서비스 제공 여부 확인 및 서비스 제공 시 통화 내용 분석 내용 상단 알림을 통한 피싱 경고 기능
- 피싱 알림 기록들을 보여주는 기능
- 연락처에 등록된 번호 및 발신 전화는 자동으로 서비스 제공 X
- 녹음 파일 클릭 시 텍스트 파일로 변환 및 텍스트 파일 클릭 시 내용 열람 기능
- 키패드 입력을 통한 전화 발신 기능
- 연락처 열람 및 검색, 연락처 내 전화&문자 기능
- 부팅 시 자동 실행 기능

#### 보이스 피싱 상단 알림
![Phishing_Alert](https://github.com/user-attachments/assets/884e28ba-6c81-4680-acd2-cbaa988e8686)

#### 보이스 피싱 알림 기록 
![Phishing_Log](https://github.com/user-attachments/assets/cb952c4c-5c63-46f8-9f25-98e49e19c870)

## 사용 기술
- REST API를 활용한 Clova Speech AI & Gpt OpenAI와 Request & Response 통신으로 통화 내용 분석(Android 보안 정책으로 단순 HTTP 통신은 불가)
- 내장 DB DAO 기술을 활용하여 피싱 알림 내역을 관리


## 전체 실행 영상
https://github.com/user-attachments/assets/f16eee4a-79b5-40eb-bfe4-fc8679add174
