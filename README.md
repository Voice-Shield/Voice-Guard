# Voice Guard
## 보이스 피싱 방지 어플
- Clova Speech API와 OpenAI의 Gpt API를 통해 수신 통화 내용을 분석하여 사용자에게 피싱 의심 전화 수신 시 경고 알림

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
- REST API를 활용한 Clova Speech AI & Gpt AI와 Request & Response 통신으로 통화 내용 분석
- 내장 DB DAO 기술을 활용하여 피싱 알림 내역을 관리

## 한계점 및 필요 보완 사항
- Android 보안 패치로 인하여 시스템 앱이 아닌 경우 통화 중 백그라운드 작업이 불가하여 실시간 알림이 불가(현재는 통화 종료 후에 경고 알림)
- 추후 해당 한계점을 우회할 수 있는 방안을 찾아 기능 향상 예정

## 전체 실행 영상
https://github.com/user-attachments/assets/f16eee4a-79b5-40eb-bfe4-fc8679add174
