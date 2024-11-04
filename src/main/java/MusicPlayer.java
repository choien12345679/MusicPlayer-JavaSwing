import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;

public class MusicPlayer extends JFrame {
    private Clip clip;
    private int currentTrack = 0;
    private boolean isPaused = false; // 일시정지 상태를 확인하는 플래그
    private boolean isStopped = false; // 정지 상태를 확인하는 플래그
    private String[] song = {"audio/addiction.wav", "audio/apt.wav", "audio/supersonic.wav", "audio/supernova.wav", "audio/fakeidol.wav"};
    private String[] image = {"image/addiction.png", "image/apt.png", "image/supersonic.png", "image/supernova.jpg", "image/fakeidol.jpg"};
    private JLabel albumLabel, timeLabel;
    private JSlider slider;
    private Timer timer; // 타이머를 이용하여 시간을 갱신

    public MusicPlayer() {
        setTitle("Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        // JFrame 배경색 설정
        c.setBackground(Color.WHITE);

        // 첫 번째 앨범 이미지 표시 (크기 조정 추가)
        albumLabel = new JLabel(scaleImage(image[currentTrack], 300, 300)); // 300x300 크기로 조정
        c.add(albumLabel, BorderLayout.NORTH); // 앨범 이미지를 상단에 배치

        // 시간 표시 라벨 및 슬라이더 추가
        JPanel sliderPanel = new JPanel(new BorderLayout());
        timeLabel = new JLabel("00:00 / 00:00", JLabel.RIGHT); // 현재 시간과 총 시간을 표시
        timeLabel.setOpaque(true);  // JLabel의 배경색을 적용하기 위해 setOpaque(true)를 호출
        timeLabel.setBackground(Color.WHITE);  // JLabel 배경을 흰색으로 설정
        timeLabel.setForeground(Color.BLACK);  // 글자 색상을 검정으로 설정

        slider = new JSlider(0, 100, 0); // 초기값 0, 최대값 100, 시작 위치 0

        // JSlider UI 수정 (슬라이더의 색상 변경)
        slider.setUI(new BasicSliderUI(slider) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(240, 239, 239)); // 트랙 색상 설정
                g2d.fillRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height);
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(200, 199, 199)); // 노브 색상 설정
                g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
            }
        });

        slider.setBackground(Color.WHITE); // 슬라이더 배경색 설정
        slider.setForeground(Color.BLACK); // 틱 마크 색상 설정

        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setEnabled(false); // 음악이 로드되기 전까지 비활성화

        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(timeLabel, BorderLayout.EAST);
        c.add(sliderPanel, BorderLayout.CENTER); // 슬라이더를 이미지 아래, 버튼 위에 배치

        // 버튼 패널 생성
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        c.add(buttonPanel, BorderLayout.SOUTH); // 버튼을 가장 아래에 배치

        // 이전 항목 재생 버튼
        buttonPanel.add(createButton("image/skip-back.png", e -> playPreviousTrack()));

        // 재생 버튼
        buttonPanel.add(createButton("image/play.png", e -> playMusic()));

        // 일시정지 버튼
        buttonPanel.add(createButton("image/pause.png", e -> pauseMusic()));

        // 정지 버튼
        buttonPanel.add(createButton("image/stop.png", e -> stopMusic()));

        // 다음 항목 재생 버튼
        buttonPanel.add(createButton("image/skip-forward.png", e -> playNextTrack()));

        setSize(300, 400);
        setVisible(true);

        // 슬라이더를 움직이면 해당 위치로 재생 위치 변경
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (slider.getValueIsAdjusting() && clip != null) {
                    int newFrame = (int) ((slider.getValue() / 100.0) * clip.getFrameLength());
                    clip.setFramePosition(newFrame);
                    updateSliderAndTime(); // 시간 표시 갱신
                }
            }
        });

        // 첫 번째 트랙은 로드만 하고 재생은 하지 않음
        loadAudio(song[currentTrack]); // 음악 로드만 수행
    }

    // 이미지를 스케일링하여 JLabel에 표시하는 메서드
    private ImageIcon scaleImage(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage();
        Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    // 버튼 생성 메서드
    private JButton createButton(String imagePath, ActionListener actionListener) {
        ImageIcon icon = scaleImage(imagePath, 26, 26); // 버튼 이미지 크기 조정
        JButton button = new JButton(icon);
        button.addActionListener(actionListener);
        return button;
    }

    private void loadAudio(String pathName) {
        try {
            File audioFile = new File(pathName);
            final AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            clip = AudioSystem.getClip();
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP && !isPaused && !isStopped) { // 일시정지 및 정지 상태가 아니면 다음 곡으로 넘어감
                    playNextTrack(); // 음악이 끝났을 때 자동으로 다음 곡 재생
                }
            });
            clip.open(audioStream);
            slider.setMaximum(100);
            slider.setValue(0);
            slider.setEnabled(true); // 슬라이더 활성화
            updateSliderAndTime(); // 시간 표시 업데이트

            // 타이머를 설정하여 재생 시간 업데이트
            timer = new Timer(1000, e -> updateSliderAndTime());
            timer.start();
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    private void updateSliderAndTime() {
        if (clip != null) {
            long currentFrame = clip.getFramePosition();
            long totalFrames = clip.getFrameLength();
            long frameRate = (long) clip.getFormat().getFrameRate();  // frameRate를 long으로 변환

            long currentSecond = currentFrame / frameRate;
            long totalSeconds = totalFrames / frameRate;

            // 슬라이더 업데이트 (현재 위치를 백분율로 표현)
            slider.setValue((int) ((currentFrame / (double) totalFrames) * 100));

            // 시간 업데이트
            timeLabel.setText(formatTime(currentSecond) + " / " + formatTime(totalSeconds));
        }
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", minutes, sec);
    }

    private void playMusic() {
        if (clip != null && !clip.isRunning()) {
            isPaused = false; // 일시정지 상태 해제
            isStopped = false; // 정지 상태 해제
            clip.start();
            if (timer != null) {
                timer.start(); // 타이머 다시 시작
            }
        }
    }

    private void pauseMusic() {
        if (clip != null && clip.isRunning()) {
            isPaused = true; // 일시정지 상태로 설정
            clip.stop();
            if (timer != null) {
                timer.stop(); // 타이머 정지
            }
        }
    }

    private void stopMusic() {
        if (clip != null) {
            isPaused = false; // 정지할 때 일시정지 상태를 해제
            isStopped = true; // 정지 상태 설정
            clip.stop();
            clip.setFramePosition(0); // 재생 위치를 처음으로 설정
            slider.setValue(0); // 슬라이더 위치 초기화
            if (timer != null) {
                timer.stop(); // 타이머 정지
            }
            updateSliderAndTime(); // 시간 표시 갱신
        }
    }

    private void playPreviousTrack() {
        stopMusic();
        currentTrack = (currentTrack - 1 + song.length) % song.length;
        loadAudio(song[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300)); // 앨범 이미지 업데이트
        playMusic(); // 이전 트랙 로드 후 자동 재생
    }

    private void playNextTrack() {
        stopMusic();
        currentTrack = (currentTrack + 1) % song.length;
        loadAudio(song[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300)); // 앨범 이미지 업데이트
        playMusic(); // 다음 트랙 로드 후 자동 재생
    }

    public static void main(String[] args) {
        new MusicPlayer();
    }
}
