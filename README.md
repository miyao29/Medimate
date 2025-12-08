# MEDIMATE (메디메이트)
**< 디지털 소외계층을 위한 OCR·AI 기반 복약 관리 플랫폼 >**

MEDIMATE는 시각적 제약으로 인해 약봉투나 처방전 확인이 어려운 시각장애인 및 고령층을 위해 개발된 안드로이드 애플리케이션입니다. 

Google ML Kit OCR과 OpenAI GPT-4o를 결합하여 복약 정보를 정밀하게 분석하고, TTS(음성 합성)로 읽어주며, 

복약 알람 및 영양제 추천 기능을 통해 통합적인 건강 관리를 지원합니다.


## 개발 환경

이 프로젝트는 다음 환경에서 개발되었습니다.

- **Language**: Java
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 36 (Android 16)
- **IDE**: Android Studio Narwhal (2025.1.1 Patch 1)
- **Architecture**: MVVM Pattern (ViewModel, LiveData, Repository)


## 주요 라이브러리 & API 

- **AI & Vision**:
  - Google ML Kit: 약봉투 텍스트 추출 (OCR)
  - OpenAI GPT-4o API: OpenAI API 통신 및 JSON 파싱
  - Google TTS : 텍스트 음성 변환
- **Camera & Image**:
  - CameraX, uCrop: 카메라 촬영 및 이미지 편집
  - Glide: 이미지 로딩 및 처리
- **Database**:
  - Room Database	: 로컬 복약 기록 저장 (오프라인 지원)
  - Firebase Firestore :	사용자 데이터 클라우드 동기화
- **Network**:
  - Retrofit2 & Gson	: REST API 통신 (GPT, 네이버, 공공데이터)
- **Auth**:
  - Firebase Auth:	소셜 로그인



## 주요 기능
#### 1. 스마트 약봉투/처방전 인식 (AI Core)
- 촬영 가이드 & 크롭: 약봉투만 정확히 인식할 수 있도록 촬영 가이드라인과 uCrop 기반 이미지 자르기 기능을 제공합니다.

- 하이브리드 분석 (OCR + GPT-4o):

  ML Kit OCR: 이미지에서 텍스트 좌표를 정밀하게 추출합니다.

  GPT-4o: 뒤죽박죽인 텍스트 행(Row)을 바로잡고, 약 이름/효능/복용법/주의사항으로 구조화(JSON)합니다.

- 이원화 안내 (Dual Output):

  화면(Visual): "식후 30분" 처럼 간결한 요약 정보를 보여줍니다.

  음성(TTS): "이 약은 식사하시고 30분 뒤에 드세요" 처럼 대화형으로 친절하게 읽어줍니다.

#### 2. 복약 알람 및 기록 (Management)
- 캘린더 뷰: 주간/월간 캘린더를 통해 복약 일정을 한눈에 확인합니다.

- 스마트 알람: AlarmManager를 활용하여 앱이 종료된 상태에서도 정확한 시간에 복약 알림을 전송합니다.

- 복용 체크: 알약을 터치하여 복용 여부를 기록하고, 데이터베이스에 저장하여 관리합니다.

#### 3. 맞춤 영양제 추천 (Recommendation)
- 증상 기반 추천: 사용자의 건강 상태에 맞춰 식약처 공공데이터 기반의 영양제를 추천합니다.

- 구매 연동: 네이버 쇼핑 API를 연동하여 추천받은 영양제의 최저가 정보와 구매 링크를 제공합니다.

#### 4. 사용자 관리 (User System)
- 간편 로그인: 구글, 전화번호 인증을 통한 간편 로그인을 지원합니다 (Firebase Auth).

- 데이터 동기화: Firestore를 통해 기기를 변경해도 사용자의 복약 기록과 설정이 유지됩니다.


## 참고사항

### 1. API Key 설정 (local.properties)
이 프로젝트는 OpenAI, 네이버, 공공데이터 API Key가 필요합니다. 보안을 위해 키는 Git에 포함되지 않았으므로, 아래 설정이 필수입니다. 

프로젝트 루트 폴더(최상위)에 있는 'local.properties' 파일을 열고, 아래와 같이 본인의 OpenAI API 키를 추가해 주세요.

Properties

'sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk'
#### 아래 줄을 추가하세요 (따옴표 없이 입력)
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxx

NAVER_CLIENT_ID=xxxxxxxxxxxxxxxxxxxx

NAVER_CLIENT_SECRET=xxxxxxxxxx

PUBLIC_DATA_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

### 2. SHA 인증서 지문 등록
debug 버전으로 구글 및 전화번호 회원가입시 firebase에 인증서 지문 등록이 필수적입니다.

테스트를 위해서는 테스트용 번호 및 인증번호(전화번호: 010-1234-5678 / 인증 번호: 123456)으로  실행하거나 apk 파일로 다운 받아서 release 버전으로 실행해야 합니다.


## 팀원 및 역할
신지은	UI·UX	
- 전체 UI/UX 디자인 (Figma) 및 스플래시/온보딩 구현
- 사용자 친화적 인터페이스 설계 (고령층 타겟)

박가은	PO / AI Logic	
- GPT 프롬프트 엔지니어링 (정보 뒤섞임 방지 로직)
- OCR-GPT 데이터 파이프라인 및 TTS 이원화 로직 구현

박도연	AI Pipeline	
- OCR 모듈 이식 및 카메라/갤러리 연동
- 촬영 가이드 & uCrop 크롭 기능 구현으로 인식률 개선

정영미	Feature	
- Room DB & AlarmManager 기반 복약 알림 구현
- 식약처/네이버 API 연동 영양제 추천 시스템 개발

신정민	Auth / DB	
- Firebase Auth 연동 (구글/전화번호 로그인)
- Firestore DB 설계 및 사용자 정보 관리
