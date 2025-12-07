package com.example.medimate.OCR;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.google.mlkit.vision.text.Text.TextBlock;
import java.util.ArrayList; // ArrayList import 추가

public class OcrProcessor {

    // 1. 작업 완료 후 MainActivity에 알릴 '설계도' 정의
    public interface OcrCallback {
        void onSuccess(String rawText); // 성공 시 텍스트 전달
        void onError(String errorMessage); // 실패 시 메시지 전달
    }

    private TextRecognizer recognizer;

    public OcrProcessor() {
        // 2. 생성될 때 한글 인식기 초기화
        recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
    }

    // 3. '이미지 처리' 기능 (외부에서 호출할 함수)
    public void processBitmap(Bitmap bitmap, OcrCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    // 4. 성공하면 좌표 기반으로 정렬된 텍스트를 전달
                    String sortedText = getSortedText(visionText);
                    callback.onSuccess(sortedText);
                })
                .addOnFailureListener(e -> {
                    // 5. 실패하면 콜백으로 에러 전달
                    Log.e("OCR_PROC", "Text recognition failed", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * OCR 결과를 좌표 기반으로 정렬하여 텍스트를 재구성합니다.
     */
    private String getSortedText(Text visionText) {
        // ⭐ 수정: getTextBlocks()의 결과를 수정 가능한 리스트(ArrayList)로 복사합니다.
        List<TextBlock> blocks = new ArrayList<>(visionText.getTextBlocks());

        if (blocks.isEmpty()) {
            return "";
        }

        // 블록을 Y축 좌표 (행) 기준으로 정렬하고, Y좌표가 비슷하면 X축 좌표 (열) 기준으로 정렬합니다.
        // 이제 'blocks'는 수정 가능한 리스트이므로 sort 호출에 문제가 없습니다.
        Collections.sort(blocks, new Comparator<TextBlock>() {
            private static final int Y_AXIS_THRESHOLD = 40;

            @Override
            public int compare(TextBlock b1, TextBlock b2) {
                Rect rect1 = b1.getBoundingBox();
                Rect rect2 = b2.getBoundingBox();

                if (rect1 == null || rect2 == null) {
                    return 0;
                }

                int yDiff = rect1.top - rect2.top;

                if (Math.abs(yDiff) < Y_AXIS_THRESHOLD) {
                    // X축 비교 (보조 정렬)
                    return rect1.left - rect2.left;
                }

                // Y축 비교 (주요 정렬)
                return yDiff;
            }
        });

        // 정렬된 블록을 합쳐서 새로운 문자열을 만듭니다.
        StringBuilder sb = new StringBuilder();
        for (TextBlock block : blocks) {
            // TextBlock마다 줄바꿈을 두 번 넣어 행 구분을 명확히 합니다.
            sb.append(block.getText()).append("\n\n");
        }

        return sb.toString().trim();
    }
}