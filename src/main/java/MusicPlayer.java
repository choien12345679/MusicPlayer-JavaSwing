import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * 추가기능: 키보드 왼쪽, 오른쪽 입력 5초 뒤, 앞 이동, 스패아스바 입력 일시정지, 재생 이벤트
 * JList로 재생목록 구현, 재생목록 더블클릭 시 재생 이벤트 구현
 * 버튼을 이미지 버튼으로 대체
 * 폰트 CookieRun 폰트로 디자인 수정
 */
public class MusicPlayer extends JFrame {
    private Clip clip; // 오디오 재생을 위한 Clip 객체
    private int currentTrack = 0; // 현재 재생 중인 트랙 인덱스
    private boolean isPaused = false; // 일시 정지 여부
    private boolean isStopped = false; // 정지 여부
    private String[] song = {"audio/addiction.wav", "audio/apt.wav", "audio/supersonic.wav", "audio/supernova.wav", "audio/fakeidol.wav", "audio/MyNameIs.wav", "audio/MaskOn.wav"}; // 재생할 오디오 파일 경로
    private String[] image = {"image/addiction.png", "image/apt.png", "image/supersonic.png", "image/supernova.jpg", "image/fakeidol.jpg", "image/MyNameIs.png", "image/MaskOn.png"}; // 트랙 별 앨범 이미지 경로
    private String[] songTitles = {"고민중독", "Apt", "Supersonic", "Supernova", "가짜 아이돌", "My name is", "Mask On"}; // 트랙 제목
    private JLabel albumLabel, timeLabel; // 앨범 이미지 및 재생 시간 표시를 위한 레이블
    private JSlider slider; // 재생 위치를 나타내는 슬라이더
    private Timer timer; // 재생 시간 업데이트를 위한 타이머
    private JList<String> playlist; // 트랙 목록을 표시하는 리스트

    /**
     * MusicPlayer 생성자: UI 구성 및 첫 트랙 로드
     */
    public MusicPlayer() {
        setTitle("Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 사용자 정의 폰트 로드
        Font customFont = loadCustomFont("font/CookieRun.ttf", 14f);

        // 앨범 이미지를 포함하는 왼쪽 패널
        JPanel playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBackground(Color.WHITE);

        // 앨범 이미지 표시
        albumLabel = new JLabel(scaleImage(image[currentTrack], 300, 300));
        playerPanel.add(albumLabel, BorderLayout.NORTH);

        // 슬라이더와 재생 시간 표시 패널
        JPanel sliderPanel = new JPanel(new BorderLayout());
        JLabel songTitleLabel = new JLabel(songTitles[currentTrack], JLabel.CENTER);
        songTitleLabel.setBackground(Color.WHITE);
        songTitleLabel.setOpaque(true);
        songTitleLabel.setFont(loadCustomFont("font/CookieRun.ttf", 18f));
        sliderPanel.add(songTitleLabel, BorderLayout.NORTH);
        timeLabel = new JLabel("00:00 / 00:00", JLabel.RIGHT);
        timeLabel.setOpaque(true);
        timeLabel.setBackground(Color.WHITE);
        timeLabel.setForeground(Color.BLACK);
        timeLabel.setFont(customFont);

        // 슬라이더 설정
        slider = new JSlider(0, 100, 0);
        slider.setUI(new CustomSliderUI(slider));
        slider.setBackground(Color.WHITE);
        slider.setForeground(Color.BLACK);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setEnabled(false);

        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(timeLabel, BorderLayout.EAST);
        playerPanel.add(sliderPanel, BorderLayout.CENTER);

        // 재생, 일시 정지, 다음/이전 트랙 등 컨트롤 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(createButton("image/skip-back.png", e -> playPreviousTrack()));
        buttonPanel.add(createButton("image/play.png", e -> playMusic()));
        buttonPanel.add(createButton("image/pause.png", e -> pauseMusic()));
        buttonPanel.add(createButton("image/stop.png", e -> stopMusic()));
        buttonPanel.add(createButton("image/skip-forward.png", e -> playNextTrack()));

        playerPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 재생 목록 패널
        playlist = new JList<>(songTitles);
        playlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlist.setSelectedIndex(currentTrack);
        playlist.setFont(customFont);
        playlist.setFixedCellHeight(60);
        playlist.setFixedCellWidth(200);

        playlist.setCellRenderer(new CustomCellRenderer());

        JScrollPane playlistScrollPane = new JScrollPane(playlist);
        playlistScrollPane.setPreferredSize(new Dimension(200, 0));

        // 왼쪽과 오른쪽 패널을 분할하는 JSplitPane 설정
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playerPanel, playlistScrollPane);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        // 재생 목록에서 트랙을 더블 클릭하여 재생
        playlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    currentTrack = playlist.getSelectedIndex();
                    playSelectedTrack();
                }
            }
        });

        // 키보드 이벤트 리스너 추가
        addKeyListener(new CustomKeyListener());
        setFocusable(true);
        requestFocusInWindow();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                requestFocusInWindow();
            }
        });

        setSize(600, 446);
        setVisible(true);

        slider.addChangeListener(new SliderChangeListener());

        // 첫 트랙 로드
        loadAudio(song[currentTrack]);
    }

    /**
     * 폰트를 로드하는 메서드
     */
    private Font loadCustomFont(String path, float size) {
        try {
            InputStream is = new FileInputStream(path);
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            return font.deriveFont(size);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            return new Font("Arial", Font.PLAIN, (int) size);
        }
    }

    /**
     * 이미지를 원하는 크기로 스케일링하는 메서드
     */
    private ImageIcon scaleImage(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage();
        Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    /**
     * 이미지를 기반으로 버튼을 생성하는 메서드
     */
    private JButton createButton(String imagePath, ActionListener actionListener) {
        JButton button = new JButton(scaleImage(imagePath, 26, 26));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(actionListener);
        return button;
    }

    /**
     * 오디오 파일을 로드하고 클립을 초기화하는 메서드
     */
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

    /**
     * 음악 재생 메서드
     */
    private void playMusic() {
        requestFocusInWindow();
        if (clip != null) {
            clip.start();
            isPaused = false;
            isStopped = false;
            if (timer != null) timer.start();
        }
    }

    /**
     * 슬라이더와 시간 레이블을 업데이트하는 메서드
     */
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

    /**
     * 음악을 일시 정지하는 메서드
     */
    private void pauseMusic() {
        if (clip != null && clip.isRunning()) {
            isPaused = true;
            clip.stop();
            if (timer != null) timer.stop();
        }
    }

    /**
     * 음악을 정지하는 메서드
     */
    private void stopMusic() {
        if (clip != null) {
            isStopped = true;
            clip.stop();
            clip.setFramePosition(0);
            slider.setValue(0);
            if (timer != null) timer.stop();
        }
    }

    /**
     * 재생을 5초 앞으로 이동하는 메서드
     */
    private void skipForward() {
        if (clip != null) {
            int newFrame = clip.getFramePosition() + (int) (clip.getFormat().getFrameRate() * 5);
            if (newFrame < clip.getFrameLength()) {
                clip.setFramePosition(newFrame);
            } else {
                playNextTrack();
            }
            updateSliderAndTime();
        }
    }

    /**
     * 재생을 5초 뒤로 이동하는 메서드
     */
    private void skipBackward() {
        if (clip != null) {
            int newFrame = clip.getFramePosition() - (int) (clip.getFormat().getFrameRate() * 5);
            if (newFrame > 0) {
                clip.setFramePosition(newFrame);
            } else {
                clip.setFramePosition(0);
            }
            updateSliderAndTime();
        }
    }

    /**
     * 이전 트랙을 재생하는 메서드
     */
    private void playPreviousTrack() {
        currentTrack = (currentTrack - 1 + song.length) % song.length;
        playSelectedTrack();
    }

    /**
     * 다음 트랙을 재생하는 메서드
     */
    private void playNextTrack() {
        currentTrack = (currentTrack + 1) % song.length;
        playSelectedTrack();
    }

    /**
     * 선택된 트랙을 재생하는 메서드
     */
    private void playSelectedTrack() {
        requestFocusInWindow();
        stopMusic();
        loadAudio(song[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300));
        JLabel songTitleLabel = (JLabel) ((JPanel) slider.getParent()).getComponent(0);
        songTitleLabel.setText(songTitles[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300));
        playlist.setSelectedIndex(currentTrack);
        playlist.repaint();
        playMusic();
    }

    public static void main(String[] args) {
        new MusicPlayer();
    }

    /**
     * 슬라이더 UI 커스터마이징을 위한 클래스
     */
    private class CustomSliderUI extends BasicSliderUI {
        public CustomSliderUI(JSlider slider) {
            super(slider);
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(240, 239, 239));
            g2d.fillRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height);
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(new Color(200, 199, 199));
            g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
        }
    }

    /**
     * 재생 목록의 트랙을 커스터마이징된 방식으로 렌더링하는 클래스
     */
    private class CustomCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (index == currentTrack) {
                renderer.setBackground(Color.LIGHT_GRAY);
            } else {
                renderer.setBackground(isSelected ? list.getSelectionBackground() : Color.WHITE);
            }
            renderer.setOpaque(true);
            renderer.setPreferredSize(new Dimension(200, 40));
            return renderer;
        }
    }

    /**
     * 키보드 입력을 통해 재생, 일시 정지 및 탐색을 제어하는 클래스
     */
    private class CustomKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    if (clip != null) {
                        if (clip.isRunning()) pauseMusic();
                        else playMusic();
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    skipForward();
                    break;
                case KeyEvent.VK_LEFT:
                    skipBackward();
                    break;
            }
        }
    }

    /**
     * 클립 재생 완료 후 자동으로 다음 트랙을 재생하는 클래스
     */
    private class ClipLineListener implements LineListener {
        @Override
        public void update(LineEvent e) {
            if (e.getType() == LineEvent.Type.STOP && !isPaused && !isStopped) {
                playNextTrack();
            }
        }
    }

    /**
     * 슬라이더 위치가 변경될 때 재생 위치를 업데이트하는 클래스
     */
    private class SliderChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (slider.getValueIsAdjusting() && clip != null) {
                int newFrame = (int) ((slider.getValue() / 100.0) * clip.getFrameLength());
                clip.setFramePosition(newFrame);
                updateSliderAndTime();
            }
        }
    }
}
