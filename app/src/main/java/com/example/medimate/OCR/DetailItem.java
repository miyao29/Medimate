package com.example.medimate.OCR;

public class DetailItem {
    // public으로 선언해서 외부에서 바로 접근 가능하게 함 (Getter 없이 편하게 사용)
    public String title;          // 버튼 제목 (예: "생김새")
    public String displayContent; // 화면 출력용 (예: "흰색 원형")
    public String ttsContent;     // 음성 출력용 (예: "이 약은 흰색의...")

    // 생성자도 3개를 받도록 변경
    public DetailItem(String title, String displayContent, String ttsContent) {
        this.title = title;
        this.displayContent = displayContent;
        this.ttsContent = ttsContent;
    }
}
