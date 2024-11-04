import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class MusicPlayer extends JFrame {
    private Clip clip;
    private int currentTrack = 0;
    private boolean isPaused = false; // 일시정지 상태 플래그
    private boolean isStopped = false; // 정지 상태 플래그
    private String[] song = {"audio/addiction.wav", "audio/apt.wav", "audio/supersonic.wav", "audio/supernova.wav", "audio/fakeidol.wav"};
    private String[] image = {"image/addiction.png", "image/apt.png", "image/supersonic.png", "image/supernova.jpg", "image/fakeidol.jpg"};
    private String[] songTitles = {"Addiction", "Apt", "Supersonic", "Supernova", "Fake Idol"};
    private JLabel albumLabel, timeLabel, titleLabel; // 제목 레이블 추가
    private JSlider slider;
    private Timer timer; // 재생 시간 업데이트를 위한 타이머
    private JList<String> playlist;

    public MusicPlayer() {
        setTitle("Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 왼쪽 패널: 앨범 이미지, 제목, 컨트롤
        JPanel playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBackground(Color.WHITE);

        // 앨범 이미지
        albumLabel = new JLabel(scaleImage(image[currentTrack], 300, 300)); // 앨범 아트를 300x300으로 스케일
        playerPanel.add(albumLabel, BorderLayout.NORTH);

        // 제목 레이블 추가
        titleLabel = new JLabel(songTitles[currentTrack], JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18)); // 제목 폰트 스타일
        playerPanel.add(titleLabel, BorderLayout.CENTER);

        // 슬라이더 및 시간 패널
        JPanel sliderPanel = new JPanel(new BorderLayout());
        timeLabel = new JLabel("00:00 / 00:00", JLabel.RIGHT); // 현재 시간과 총 시간 표시
        timeLabel.setOpaque(true);
        timeLabel.setBackground(Color.WHITE);
        timeLabel.setForeground(Color.BLACK);
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 14)); // 가독성을 높이기 위해 폰트 크기 증가

        slider = new JSlider(0, 100, 0); // 슬라이더 초기 설정
        slider.setUI(new BasicSliderUI(slider) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(240, 239, 239)); // 트랙 색상
                g2d.fillRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height);
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(200, 199, 199)); // 노브 색상
                g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
            }
        });
        slider.setBackground(Color.WHITE);
        slider.setForeground(Color.BLACK);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setEnabled(false); // 초기에는 비활성화 상태

        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(timeLabel, BorderLayout.EAST);

        // 슬라이더 패널을 플레이어 패널의 하단에 추가
        playerPanel.add(sliderPanel, BorderLayout.SOUTH);

        // 컨트롤 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(createButton("image/skip-back.png", e -> playPreviousTrack()));
        buttonPanel.add(createButton("image/play.png", e -> playMusic()));
        buttonPanel.add(createButton("image/pause.png", e -> pauseMusic()));
        buttonPanel.add(createButton("image/stop.png", e -> stopMusic()));
        buttonPanel.add(createButton("image/skip-forward.png", e -> playNextTrack()));

        // 버튼 패널을 플레이어 패널의 하단에 추가
        playerPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 오른쪽 패널: 플레이리스트
        playlist = new JList<>(songTitles);
        playlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlist.setSelectedIndex(currentTrack);
        playlist.setFont(new Font("Arial", Font.PLAIN, 14)); // 가독성을 높이기 위해 폰트 크기 증가
        playlist.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index == currentTrack) {
                    renderer.setBackground(Color.LIGHT_GRAY); // 현재 재생 중인 트랙 하이라이트
                } else {
                    renderer.setBackground(isSelected ? list.getSelectionBackground() : Color.WHITE);
                }
                renderer.setOpaque(true); // 배경 색상 표시 보장
                return renderer;
            }
        });

        JScrollPane playlistScrollPane = new JScrollPane(playlist);
        playlistScrollPane.setPreferredSize(new Dimension(200, 0)); // 플레이리스트 패널의 크기 조정

        // 왼쪽과 오른쪽 패널을 균등하게 나누기 위한 스플릿 패널
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playerPanel, playlistScrollPane);
        splitPane.setDividerLocation(300); // 50-50 분할 위치 설정
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        // 플레이리스트 이벤트 핸들링
        playlist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 더블 클릭으로 선택된 트랙 재생
                    currentTrack = playlist.getSelectedIndex();
                    playSelectedTrack();
                }
            }
        });

        // 키보드 컨트롤 설정
        setupKeyboardControls();

        setSize(600, 400); // 균형 잡힌 크기로 조정
        setVisible(true);

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (slider.getValueIsAdjusting() && clip != null) {
                    int newFrame = (int) ((slider.getValue() / 100.0) * clip.getFrameLength());
                    clip.setFramePosition(newFrame);
                    updateSliderAndTime();
                }
            }
        });

        loadAudio(song[currentTrack]); // 초기 곡 로드
    }

    private void setupKeyboardControls() {
        // SPACE로 재생/일시정지, 화살표 키로 앞으로/뒤로 건너뛰기
        InputMap inputMap = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "playPause");
        actionMap.put("playPause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clip != null) {
                    if (clip.isRunning()) pauseMusic();
                    else playMusic();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "skipForward");
        actionMap.put("skipForward", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                skipForward(); // 5초 앞으로 이동
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "skipBackward");
        actionMap.put("skipBackward", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                skipBackward(); // 5초 뒤로 이동
            }
        });
    }

    private ImageIcon scaleImage(String path, int width, int height) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage();
        Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    private JButton createButton(String imagePath, ActionListener actionListener) {
        ImageIcon icon = scaleImage(imagePath, 26, 26);
        JButton button = new JButton(icon);
        button.addActionListener(actionListener);
        return button;
    }

    private void loadAudio(String pathName) {
        try {
            File audioFile = new File(pathName);
            final AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    playNextTrack(); // 곡이 끝나면 다음 곡 재생
                }
            });
            clip.open(audioStream);
            slider.setEnabled(true); // 오디오 로드 후 슬라이더 활성화
            updateSliderAndTime();

            // 타이머 설정하여 주기적으로 슬라이더와 시간을 업데이트
            timer = new Timer(1000, e -> updateSliderAndTime());
            timer.start();

        } catch (Exception e) {
            e.printStackTrace(); // 예외 처리
        }
    }

    private void playMusic() {
        if (clip != null) {
            if (isStopped) {
                clip.setFramePosition(0); // 정지 후 재생 시 처음부터 시작
                isStopped = false;
            }
            clip.start();
            isPaused = false; // 재생 상태 업데이트
        }
    }

    private void pauseMusic() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            isPaused = true; // 일시정지 상태 업데이트
        }
    }

    private void stopMusic() {
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            isStopped = true; // 정지 상태 업데이트
            updateSliderAndTime();
        }
    }

    private void playNextTrack() {
        currentTrack = (currentTrack + 1) % song.length; // 다음 트랙으로 이동
        loadAudio(song[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300));
        titleLabel.setText(songTitles[currentTrack]); // 제목 업데이트
        playlist.setSelectedIndex(currentTrack); // 현재 트랙 선택 표시
    }

    private void playPreviousTrack() {
        currentTrack = (currentTrack - 1 + song.length) % song.length; // 이전 트랙으로 이동
        loadAudio(song[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300));
        titleLabel.setText(songTitles[currentTrack]); // 제목 업데이트
        playlist.setSelectedIndex(currentTrack); // 현재 트랙 선택 표시
    }

    private void updateSliderAndTime() {
        if (clip != null) {
            int totalFrames = clip.getFrameLength();
            int currentFrame = clip.getFramePosition();

            slider.setValue((int) ((currentFrame / (double) totalFrames) * 100)); // 슬라이더 업데이트
            timeLabel.setText(formatTime(currentFrame) + " / " + formatTime(totalFrames)); // 시간 업데이트
        }
    }

    private String formatTime(int frame) {
        int seconds = frame / (int) clip.getFormat().getFrameRate();
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%02d:%02d", minutes, seconds); // MM:SS 형식으로 반환
    }

    private void skipForward() {
        if (clip != null) {
            // 5초 앞으로 이동
            int framesToSkip = (int) (clip.getFormat().getFrameRate() * 5);
            int newFrame = clip.getFramePosition() + framesToSkip;
            if (newFrame < clip.getFrameLength()) {
                clip.setFramePosition(newFrame);
            } else {
                // 클립의 끝을 넘을 경우 다음 트랙 재생
                playNextTrack();
            }
        }
    }

    private void skipBackward() {
        if (clip != null) {
            // 5초 뒤로 이동
            int framesToSkip = (int) (clip.getFormat().getFrameRate() * 5);
            int newFrame = clip.getFramePosition() - framesToSkip;
            if (newFrame > 0) {
                clip.setFramePosition(newFrame);
            } else {
                // 클립의 시작을 넘을 경우 처음으로 이동
                clip.setFramePosition(0);
            }
        }
    }

    private void playSelectedTrack() {
        loadAudio(song[currentTrack]);
        albumLabel.setIcon(scaleImage(image[currentTrack], 300, 300));
        titleLabel.setText(songTitles[currentTrack]); // 제목 업데이트
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MusicPlayer::new); // GUI 초기화
    }
}
