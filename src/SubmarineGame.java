import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class SubmarineGame extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 1900;
    int boardHeight = 720;

    // Images
    Image backgroundImg, submarineImg, coralTopImg, coralBottomImg, fishImg, fullHeartImg, blankHeartImg;

    // Parallax background properties
    int[] backgroundX = {0, 1900}; // Two instances of the background for seamless scrolling
    int backgroundSpeed = 2;       // Speed of the background

    // Submarine properties
    int submarineX = boardWidth / 8;
    int submarineY = boardHeight / 2;
    int submarineWidth = 80;
    int submarineHeight = 45;

    class Submarine {
        int x = submarineX;
        int y = submarineY;
        int width = submarineWidth;
        int height = submarineHeight;
        Image img;

        Submarine(Image img) {
            this.img = img;
        }
    }

    // Coral/obstacle properties
    int coralX = boardWidth;
    int coralY = 0;
    int coralWidth = 60;
    int coralHeight = 500;

    class Coral {
        int x = coralX;
        int y = coralY;
        int width = coralWidth;
        int height = coralHeight;
        Image img;
        boolean passed = false;

        Coral(Image img) {
            this.img = img;
        }
    }

    // Fish properties
    class Fish {
        int x, y, width, height;
        Image img;

        Fish(Image img, int x, int y, int width, int height) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    // Game logic
    Submarine submarine;
    int velocityX = -10;
    int velocityY = 0;
    int gravity = 1;
    int buoyancy = -1;

    ArrayList<Coral> corals;
    ArrayList<Fish> fishes;

    Random random = new Random();
    Timer gameLoop, coralPlacementTimer, fishPlacementTimer;
    boolean gameOver = false;
    boolean gameStarted = false;
    double score = 0;

    int lives = 3; // Player lives

    JButton startButton;

    // Sound file paths
    String collisionSoundPath = "./collision.wav";
    String gameOverSoundPath = "./gameover.wav";

    public SubmarineGame() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.white);
        setFocusable(true);
        addKeyListener(this);

        // Load images
        backgroundImg = new ImageIcon(getClass().getResource("./submarine_background.png")).getImage();
        submarineImg = new ImageIcon(getClass().getResource("./submarine.png")).getImage();
        coralTopImg = new ImageIcon(getClass().getResource("./coral_top.png")).getImage();
        coralBottomImg = new ImageIcon(getClass().getResource("./coral_bottom.png")).getImage();
        fishImg = new ImageIcon(getClass().getResource("./fish.png")).getImage();
        fullHeartImg = new ImageIcon(getClass().getResource("./full_heart.png")).getImage();
        blankHeartImg = new ImageIcon(getClass().getResource("./blank_heart.png")).getImage();

        // Initialize submarine, corals, and fishes
        submarine = new Submarine(submarineImg);
        corals = new ArrayList<>();
        fishes = new ArrayList<>();

        // Setup Start button
        startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.PLAIN, 24));
        startButton.addActionListener(e -> startGame());
        add(startButton);

        // Timer to place coral obstacles
        coralPlacementTimer = new Timer(1500, e -> placeCorals());

        // Timer to place fishes
        fishPlacementTimer = new Timer(2000, e -> placeFish());

        // Main game loop timer
        gameLoop = new Timer(16, this);
    }

    private void startGame() {
        gameStarted = true;
        startButton.setVisible(false); // Hide the start button
        coralPlacementTimer.start();
        fishPlacementTimer.start();
        gameLoop.start();
    }

    public void placeCorals() {
        int randomY = (int) (coralY - coralHeight / 4 - Math.random() * (coralHeight / 2));
        int gap = boardHeight / 3;

        Coral topCoral = new Coral(coralTopImg);
        topCoral.y = randomY;
        corals.add(topCoral);

        Coral bottomCoral = new Coral(coralBottomImg);
        bottomCoral.y = topCoral.y + coralHeight + gap;
        corals.add(bottomCoral);
    }

    public void placeFish() {
        int randomX = boardWidth + random.nextInt(300);
        int randomY = random.nextInt(boardHeight - 50);
        Fish fish = new Fish(fishImg, randomX, randomY, 60, 40);
        fishes.add(fish);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // Draw parallax background
        for (int i = 0; i < backgroundX.length; i++) {
            g.drawImage(backgroundImg, backgroundX[i], 0, boardWidth, boardHeight, null);
        }

        // Draw submarine
        g.drawImage(submarine.img, submarine.x, submarine.y, submarine.width, submarine.height, null);

        // Draw corals
        for (Coral coral : corals) {
            g.drawImage(coral.img, coral.x, coral.y, coral.width, coral.height, null);
        }

        // Draw fishes
        for (Fish fish : fishes) {
            g.drawImage(fish.img, fish.x, fish.y, fish.width, fish.height, null);
        }

        drawHearts(g);

        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + (int) score, 10, 35);
            g.drawString("Press SPACE to Restart", 10, 70);
        } else if (!gameStarted) {
            g.drawString("Press 'Start Game' to Begin", boardWidth / 4, boardHeight / 2);
        } else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }
    }

    private void drawHearts(Graphics g) {
        int heartWidth = 50;
        int heartHeight = 50;
        int startX = 10;
        int startY = 70;

        for (int i = 0; i < 3; i++) {
            if (i < lives) {
                g.drawImage(fullHeartImg, startX + (i * (heartWidth + 10)), startY, heartWidth, heartHeight, null);
            } else {
                g.drawImage(blankHeartImg, startX + (i * (heartWidth + 10)), startY, heartWidth, heartHeight, null);
            }
        }
    }

    private void playSound(String soundFile) {
        try {
            URL soundURL = getClass().getResource(soundFile);
            if (soundURL == null) {
                System.err.println("Sound file not found: " + soundFile);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void move() {
        // Update background for parallax effect
        for (int i = 0; i < backgroundX.length; i++) {
            backgroundX[i] -= backgroundSpeed;
            if (backgroundX[i] + boardWidth < 0) {
                backgroundX[i] = boardWidth;
            }
        }
    
        // Update vertical submarine movement (gravity + buoyancy)
        velocityY += gravity + buoyancy;
        submarine.y += velocityY;
        submarine.y = Math.max(submarine.y, 0);  // Prevent the submarine from going above the screen
    
        // Check for collisions with corals and fishes
        for (Coral coral : corals) {
            coral.x += velocityX;  // Move corals to the left
            if (!coral.passed && submarine.x > coral.x + coral.width) {
                coral.passed = true;
                score += 0.5;
            }
    
            if (collision(submarine, coral)) {
                lives--;
                corals.remove(coral);
                playSound(collisionSoundPath);
    
                if (lives <= 0) {
                    gameOver = true;
                    playSound(gameOverSoundPath);
                }
                break;
            }
        }
    
        for (Fish fish : fishes) {
            fish.x += velocityX;  // Move fishes to the left
            if (collision(submarine, fish)) {
                lives--;
                fishes.remove(fish);
                playSound(collisionSoundPath);
    
                if (lives <= 0) {
                    gameOver = true;
                    playSound(gameOverSoundPath);
                }
                break;
            }
        }
    
        // Remove fishes that are off the screen
        fishes.removeIf(fish -> fish.x + fish.width < 0);
    
        if (submarine.y > boardHeight) {
            gameOver = true;
            playSound(gameOverSoundPath);
        }
    }
    

    private boolean collision(Submarine submarine, Coral coral) {
        return new Rectangle(submarine.x, submarine.y, submarine.width, submarine.height)
                .intersects(new Rectangle(coral.x, coral.y, coral.width, coral.height));
    }

    private boolean collision(Submarine submarine, Fish fish) {
        return new Rectangle(submarine.x, submarine.y, submarine.width, submarine.height)
                .intersects(new Rectangle(fish.x, fish.y, fish.width, fish.height));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameStarted && !gameOver) {
            move();
            repaint();
        }
    }

    @Override
public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
        if (gameOver) {
            resetGame();
        } else {
            velocityY = buoyancy; // Make the submarine go up
        }
    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
        submarine.y -= 20;  // Move up
        if (submarine.y < 0) submarine.y = 0;  // Prevent going out of bounds
    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
        submarine.y += 20;  // Move down
        if (submarine.y > boardHeight - submarine.height) {
            submarine.y = boardHeight - submarine.height;  // Prevent going out of bounds
        }
    } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
        submarine.x -= 5;  // Move left
        if (submarine.x < 0) submarine.x = 0;  // Prevent going out of bounds
    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
        submarine.x += 5;  // Move right
        if (submarine.x > boardWidth - submarine.width) {
            submarine.x = boardWidth - submarine.width;  // Prevent going out of bounds
        }
    }
}


    private void resetGame() {
        lives = 3;
        score = 0;
        gameOver = false;
        submarine.y = boardHeight / 2;
        corals.clear();
        fishes.clear();
        gameLoop.start();
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Submarine Game");
        SubmarineGame game = new SubmarineGame();
        frame.add(game);
        frame.pack();
         // place the window at the centre of the screen
         frame.setLocationRelativeTo(null);
         // by this option the user cannot resize the size of terminal
         frame.setResizable(    false);
         // when the user press x button so it will terminate the window
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         frame.setVisible(true);
    }
}
