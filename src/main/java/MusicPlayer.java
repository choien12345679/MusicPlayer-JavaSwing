import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class MusicPlayer extends JFrame {
    private Clip clip; // 현재 재생중인 오디오 클립 객체
    private int currentTrack = 0; // 현재 트랙 인덱스
    private boolean isPaused = false; // 일시정지 상태를 나타내는 플래그
    private boolean isStopped = false; // 정지 상태를 나타내는 플래그
    private String[] song = {"audio/addiction.wav", "audio/apt.wav", "audio/supersonic.wav", "audio/supernova.wav", "audio/fakeidol.wav", "audio/MyNameIs.wav", "audio/MaskOn.wav"}; // 오디오 파일 배열
    private String[] image = {"image/addiction.png", "image/apt.png", "image/supersonic.png", "image/supernova.jpg", "image/fakeidol.jpg", "image/MyNameIs.png", "image/MaskOn.png"}; // 앨범 이미지 배열
    private String[] songTitles = {"고민중독", "Apt", "Supersonic", "Supernova", "가짜 아이돌", "My name is", "Mask On"}; // 곡 제목 배열
    private JLabel albumLabel, timeLabel; // 앨범 이미지 및 재생 시간 라벨
    private JSlider slider; // 재생 위치 조절 슬라이더
    private Timer timer; // 재생 시간 업데이트용 타이머
    private JList<String> playlist; // 재생 목록 리스트

    public MusicPlayer() {
        setTitle("Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 글씨체 정의
        Font customFont = loadCustomFont("font/CookieRun.ttf", 14f);

        // 왼쪽 패널: 앨범 이미지, 시간 표시, 재생 컨트롤 패널
        JPanel playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBackground(Color.WHITE);

        // 앨범 이미지
        albumLabel = new JLabel(scaleImage(image[currentTrack], 300, 300)); // 앨범 이미지를 300x300 크기로 설정
        playerPanel.add(albumLabel, BorderLayout.NORTH);

        // 시간 및 슬라이더 패널
        JPanel sliderPanel = new JPanel(new BorderLayout());
        JLabel songTitleLabel = new JLabel(songTitles[currentTrack], JLabel.CENTER);
        songTitleLabel.setBackground(Color.WHITE);
        songTitleLabel.setOpaque(true); // 현재 곡 제목 표시
        songTitleLabel.setFont(loadCustomFont("font/CookieRun.ttf", 18f));
        sliderPanel.add(songTitleLabel, BorderLayout.NORTH);
        timeLabel = new JLabel("00:00 / 00:00", JLabel.RIGHT); // 현재 시간 및 총 시간 표시
        timeLabel.setOpaque(true);
        timeLabel.setBackground(Color.WHITE);
        timeLabel.setForeground(Color.BLACK);
        timeLabel.setFont(customFont); // 사용자 정의 폰트 적용

        // 슬라이더 초기 설정(크기, 배경색상)
        slider = new JSlider(0, 100, 0);
        slider.setUI(new CustomSliderUI(slider));
        slider.setBackground(Color.WHITE);
        slider.setForeground(Color.BLACK);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setEnabled(false); // 초기에는 비활성화, 곡 로드 후 활성화

        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(timeLabel, BorderLayout.EAST);
        playerPanel.add(sliderPanel, BorderLayout.CENTER);

        // 재생 컨트롤 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(createButton("image/skip-back.png", e -> playPreviousTrack()));
        buttonPanel.add(createButton("image/play.png", e -> playMusic()));
        buttonPanel.add(createButton("image/pause.png", e -> pauseMusic()));
        buttonPanel.add(createButton("image/stop.png", e -> stopMusic()));
        buttonPanel.add(createButton("image/skip-forward.png", e -> playNextTrack()));

        playerPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 오른쪽 패널: 재생 목록
        playlist = new JList<>(songTitles);
        playlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlist.setSelectedIndex(currentTrack);
        playlist.setFont(customFont); // 사용자 정의 폰트 적용
        playlist.setFixedCellHeight(60); // 셀 높이를 60픽셀로 설정
        playlist.setFixedCellWidth(200); // 셀 너비를 200픽셀로 설정

        playlist.setCellRenderer(new CustomCellRenderer());

        JScrollPane playlistScrollPane = new JScrollPane(playlist);
        playlistScrollPane.setPreferredSize(new Dimension(200, 0)); // 재생 목록 패널 너비 설정

        // 좌우 패널 분할
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playerPanel, playlistScrollPane);
        splitPane.setDividerLocation(300); // 분할 위치 설정
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        // 재생 목록 클릭 이벤트 처리
        playlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 더블클릭 시 선택된 트랙 재생
                    currentTrack = playlist.getSelectedIndex();
                    playSelectedTrack();
                }
            }
        });

        // 키보드 컨트롤 추가
        addKeyListener(new CustomKeyListener());
        setFocusable(true);
        requestFocusInWindow();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                requestFocusInWindow();
            }
        });

        setSize(600, 446); // 초기 프레임 크기 설정
        setVisible(true);

        slider.addChangeListener(new SliderChangeListener());

        loadAudio(song[currentTrack]); // 초기 곡 로드, 자동 재생은 하지 않음
    }

    private Font loadCustomFont(String path, float size) {
        try {
            InputStream is = new FileInputStream(path);
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            return font.deriveFont(size);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            return new Font("Arial", Font.PLAIN, (int) size); // 기본 폰트로 대체
        }
    }

    // 앨범 이미지 크기 조절
    private ImageIcon scaleImage(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage();
        Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    // 버튼 생성 메서드
    private JButton createButton(String imagePath, ActionListener actionListener) {
        JButton button = new JButton(scaleImage(imagePath, 26, 26));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(actionListener);
        return button;
    }

    // 오디오 로드 메서드
    private void loadAudio(String pathName) {
        try {
            File audioFile = new File(pathName);
            final AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            clip = AudioSystem.getClip();
            clip.addLineListener(new ClipLineListener());
            clip.open(audioStream);
            slider.setEnabled(true);
            updateSliderAndTime();

            timer = new Timer(1000, e -> updateSliderAndTime());
            timer.start();
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    // 재생 메서드
    private void playMusic() {
        requestFocusInWindow();
        if (clip != null) {
            clip.start();
            isPaused = false;
            isStopped = false;
            if (timer != null) timer.start();
        }
    }

    // 슬라이더 및 시간 업데이트 메서드
    private void updateSliderAndTime() {
        if (clip != null) {
            long currentFrame = clip.getFramePosition();
            long totalFrames = clip.getFrameLength();
            float frameRate = clip.getFormat().getFrameRate();
            long currentTimeInSeconds = (long) (currentFrame / frameRate);
            long totalTimeInSeconds = (long) (totalFrames / frameRate);

            slider.setValue((int) ((currentFrame * 100) / totalFrames));

            timeLabel.setText(String.format("%02d:%02d / %02d:%02d",
                    currentTimeInSeconds / 60, currentTimeInSeconds % 60,
                    totalTimeInSeconds / 60, totalTimeInSeconds % 60));
            playlist.repaint();
        }
    }

    // 재생 일시정지 메서드
    private void pauseMusic() {
        if (clip != null && clip.isRunning()) {
            isPaused = true;
            clip.stop();
            if (timer != null) timer.stop();
        }
    }

    // 음악 정지 메서드
    private void stopMusic() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            isStopped = true;
            if (timer != null) timer.stop();
            slider.setValue(0);
            timeLabel.setText("00:00 / 00:00");
        }
    }

    // 이전 트랙 재생 메서드
    private void playPreviousTrack() {
        stopMusic();
        currentTrack = (currentTrack > 0) ? currentTrack - 1 : song.length - 1;
        playSelectedTrack();
    }

    // 다음 트랙 재생 메서드
    private void playNextTrack() {
        stopMusic();
        currentTrack = (currentTrack < song.length - 1) ? currentTrack + 1 : 0;
        playSelectedTrack();
    }

    // 선택된 트랙 재생 메서드
    private void playSelectedTrack() {
        loadAudio(song[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300));
        playMusic();
    }

    // 커스텀 키 리스너
    private class CustomKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_P: playMusic(); break;
                case KeyEvent.VK_O: pauseMusic(); break;
                case KeyEvent.VK_S: stopMusic(); break;
                case KeyEvent.VK_RIGHT: playNextTrack(); break;
                case KeyEvent.VK_LEFT: playPreviousTrack(); break;
            }
        }
    }

    // 슬라이더 커스텀 UI 클래스
    private static class CustomSliderUI extends BasicSliderUI {
        public CustomSliderUI(JSlider slider) {
            super(slider);
        }

        @Override
        public void paintTrack(Graphics g) {
            g.setColor(Color.LIGHT_GRAY);
            super.paintTrack(g);
        }
    }

    // 슬라이더 위치 변경 리스너
    private class SliderChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (slider.getValueIsAdjusting() && clip != null) {
                long totalFrames = clip.getFrameLength();
                clip.setFramePosition((int) (slider.getValue() * totalFrames / 100));
                updateSliderAndTime();
            }
        }
    }

    // 오디오 클립 라인 리스너 -> 재생목록에서 곡 선택하면 기존에 플레이되던곡 멈추게 하는거
    private class ClipLineListener implements LineListener {
        @Override
        public void update(LineEvent event) {
            if (event.getType() == LineEvent.Type.STOP && !isPaused && !isStopped) {
                playNextTrack();
            }
        }
    }

    // 재생 목록 커스텀 셀 렌더러 
    private class CustomCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setFont(loadCustomFont("font/CookieRun.ttf", 16f));

            if (index == currentTrack) {
                label.setBackground(Color.GRAY);
                label.setForeground(Color.WHITE);
            }
            return label;
        }
    }

    // 메인 메서드
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MusicPlayer::new);
    }
}
