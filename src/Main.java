import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        char[] MOVES = {'L', 'X', 'U', 'R', 'Y', 'D'};
        Map<Cube, Map<Cube, Character>> cubeSpace = loadCubeSpace(MOVES);
        Scanner scanner = new Scanner(System.in);
        System.out.println("---------------------------------------------");
        System.out.println("Welcome to the mini rubik's cube solver!");
        System.out.println();
        System.out.println("PURPOSE: For any unsolved mini cube, this program provides step by step directions to solve " +
                "it using the minimum number of moves.");
        System.out.println();
        System.out.println("INPUT SPECIFICATION:");
        System.out.println("Input = a character sequence representing the configuration of the cube you want to solve. ");
        System.out.println("Each position in the sequence corresponds to a specific location on the cube.");
        System.out.println("The char at each position in the sequence corresponds to the color of the corresponding " +
                "location on the cube.");
        System.out.println("Legal chars: W=white, R=red, B=blue, O=orange, G=green, Y=yellow.");
        System.out.println("Restriction: when you enter the cube, the lower right corner of the bottom face must be " +
                "yellow, the bottom right corner of the right face must be orange, and the bottom left corner of the " +
                "back face must be green.");
        System.out.println("This restriction should be attainable with any valid cube configuration. You may need to " +
                "rotate the cube in space to figure out which faces need to be on top, middle, etc to meet the " +
                "restriction.");
        System.out.println("If it's unclear, refer to inputSpec.png for an example and visual guide.");
        System.out.println();
        System.out.println("DEFINITIONS OF EACH MOVE:");
        System.out.println("U = twist the left face counterclockwise by 90 deg.");
        System.out.println("D = twist the left face clockwise by 90 deg.");
        System.out.println("R = twist the top face counterclockwise by 90 deg.");
        System.out.println("L = twist the top face clockwise by 90 deg.");
        System.out.println("Y = twist the front face counterclockwise by 90 deg");
        System.out.println("X = twist the front face clockwise by 90 deg");
        System.out.println();

        boolean running = true;
        while(running) {
            System.out.println("Options:");
            System.out.println("1. Enter your cube's sequence to solve");
            System.out.println("2. Quit the program by entering the word \"quit\"");
            System.out.println("3. Get the solving-time and number-of-moves distribution of this tool by entering \"collect data\"");
            String input = scanner.nextLine().trim();
            if (input.equals("quit")) {
                running = false;
                System.out.println("Thank you for visiting the cube solver!");
            } else if (input.equals("collect data")) {
                System.out.println("How many cubes do you want to test? If you enter nothing, the default is 100.");
                input = scanner.nextLine().trim();
                int numPoints;
                if (input.isEmpty()) {
                    numPoints = 100;
                    collectData(cubeSpace, numPoints);
                } else if (input.matches("\\d+") && Integer.parseInt(input) > 0) {
                    numPoints = Integer.parseInt(input);
                    collectData(cubeSpace, numPoints);
                } else {
                    System.out.println("Input is invalid. For this question, you need to either press enter, or enter" +
                            "a positive integer with no + sign. Going back to main menu...");
                }

                System.out.println();
            } else {
                if (!cubeSpace.containsKey(new Cube(input))) {
                    System.out.println("Input invalid.");
                } else {
                    System.out.println("Directions: " + solveCube(input, cubeSpace));
                }
                System.out.println();
            }
        }
    }

    public static void collectData(Map<Cube, Map<Cube, Character>> cubeSpace, int numPoints) {
        Random random = new Random();
        Set<Cube> allCubes = cubeSpace.keySet();
        List<Cube> randomCubes = new ArrayList<>();

        while (randomCubes.size() < numPoints) {
            randomCubes.add(allCubes.stream().skip(random.nextInt(allCubes.size())).findFirst().orElse(null));
        }

        List<Double> times = new ArrayList<>();
        List<Integer> moves = new ArrayList<>();

        for (int i=0; i < randomCubes.size(); i++) {
            if ((i+1) % 5 == 0) {
                System.out.println("Testing " + (i+1) + "th cube");
            }
            String cube = randomCubes.get(i).toString();
            String solution;
            long start = System.nanoTime();
            solution = solveCube(cube, cubeSpace);
            long end = System.nanoTime();
            double duration = (end - start) / 1_000_000_000.0; DecimalFormat df = new DecimalFormat("#.###");
            double solveTime = Double.parseDouble(df.format(duration));
            times.add(solveTime);
            solution = solution.replaceAll("[\\[\\]]", "").trim();
            int count = solution.isEmpty() ? 0 : solution.split(", ").length;
            moves.add(count);
        }
        saveTestsInCsv(randomCubes, times, moves);

    }

    public static void saveTestsInCsv(List<Cube> randomCubes, List<Double> times, List<Integer> moves) {
        String filePath = "solveStats_" + randomCubes.size() + "Cubes.csv";
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("cube,solveTime,numMoves\n");
            for (int i = 0; i < randomCubes.size(); i++) {
                writer.append(randomCubes.get(i).toString()).append(",");
                writer.append(times.get(i).toString()).append(",");
                writer.append(moves.get(i).toString()).append("\n");
            }
            System.out.println("Solve stats saved in: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String solveCube(String string, Map<Cube, Map<Cube, Character>> cubeSpace) {
        Cube origin = new Cube(string); // creating the unsolved input cube
        Cube target = new Cube("WWWWRRRRBBBBOOOOGGGGYYYY"); // defining the destination of our search
        Queue<Cube> queue = new ArrayDeque<>();     // BFS queue
        Set<Cube> visited = new HashSet<>();        // keeps track of nodes we have already visited (and thus already
                                                    // have a shortest path for)
        Map<Cube, Cube> parent = new HashMap<>();   // Key = cube (A); Value = cube (B); (B)--> (A) when traveling from
                                                    // input --> solved
        queue.add(target);
        visited.add(target);
        parent.put(target, null);
        List<String> path = new ArrayList<>();
        while (!queue.isEmpty()) {
            Cube cube = queue.remove();
            if (cube.equals(origin)) {  // as soon as we encounter the solved state, we stop the BFS and construct the
                                        // sequence of states we traversed from input to solved.
                path = reconstructPath(parent, origin);
                break;
            }
            for (Cube neighbor : cubeSpace.get(cube).keySet()) { // if we haven't encountered the solved state yet, then
                                                                // we keep exploring, storing the predecessor of each state
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor); parent.put(neighbor, cube); queue.add(neighbor);
                }
            }
        }
        List<String> directions = getDirections(path, cubeSpace);   // retrieves the moves needed to traverse the
                                                                    // given sequence of states
        return "" + directions;
    }

    public static List<String> getDirections(List<String> path, Map<Cube, Map<Cube, Character>> cubeSpace) {
        List<String> directions = new ArrayList<>();
        for (int i = 0; i < path.size()-1; i++) {
            Cube current = new Cube(path.get(i));
            Cube next = new Cube(path.get(i+1));
            directions.add(String.valueOf(cubeSpace.get(current).get(next)));
        }
        return directions;
    }

    public static List<String> reconstructPath(Map<Cube, Cube> parent, Cube origin) {
        List<String> path = new ArrayList<>();
        Cube cube = origin;
        while (cube != null) {
            path.add(cube.toString());
            cube = parent.get(cube);
        }
        return path;
    }

    public static Map<Cube, Map<Cube, Character>> loadCubeSpace(char[] MOVES) {
        System.out.println("Processing solution space...");
        Map<Cube, Map<Cube, Character>> cubeSpace;
        String filepath = "cubeSpace.csv";
        File file = new File(filepath);
        if (file.exists() && file.isFile()) {
            // filepath exists. Loading graph from csv file...
            cubeSpace = loadFromCsv(filepath);
        } else {
            // filepath doesn't exist. Generating graph from scratch...
            cubeSpace = generateCubeSpace(MOVES);
            saveToCsv(cubeSpace, filepath);
        }
//        System.out.println("Graph contains " + cubeSpace.size() + " unique states.");
//        Set<Integer> uniqueSizes = new HashSet<>();
//        for (Map<Cube, Character> transitions : cubeSpace.values()) {
//            uniqueSizes.add(transitions.size());
//        }
//        System.out.println("Unique number of transitions from a cube: " + uniqueSizes);
//        System.out.println("Solution space finished processing");
        return cubeSpace;
    }

    public static Cube applyMove(Cube original, char move) {
        char[] copyData = new char[24];
        for (int i = 0; i < 24; i++) {
            copyData[i] = original.data[i];
        }
        switch (move) {
            case 'D':
                copyData[0] = original.data[19]; copyData[2] = original.data[17];
                copyData[8] = original.data[0]; copyData[10] = original.data[2];
                copyData[20] = original.data[8]; copyData[22] = original.data[10];
                copyData[19] = original.data[20]; copyData[17] = original.data[22];
                copyData[4] = original.data[6]; copyData[5] = original.data[4];
                copyData[6] = original.data[7]; copyData[7] = original.data[5];
                break;
            case 'U':
                copyData[0] = original.data[8]; copyData[2] = original.data[10];
                copyData[8] = original.data[20]; copyData[10] = original.data[22];
                copyData[20] = original.data[19]; copyData[22] = original.data[17];
                copyData[19] = original.data[0]; copyData[17] = original.data[2];
                copyData[4] = original.data[5]; copyData[5] = original.data[7];
                copyData[6] = original.data[4]; copyData[7] = original.data[6];
                break;
            case 'L':
                copyData[4] = original.data[8]; copyData[5] = original.data[9];
                copyData[8] = original.data[12]; copyData[9] = original.data[13];
                copyData[12] = original.data[16]; copyData[13] = original.data[17];
                copyData[16] = original.data[4]; copyData[17] = original.data[5];
                copyData[0] = original.data[2]; copyData[1] = original.data[0];
                copyData[2] = original.data[3]; copyData[3] = original.data[1];
                break;
            case 'R':
                copyData[4] = original.data[16]; copyData[5] = original.data[17];
                copyData[8] = original.data[4]; copyData[9] = original.data[5];
                copyData[12] = original.data[8]; copyData[13] = original.data[9];
                copyData[16] = original.data[12]; copyData[17] = original.data[13];
                copyData[0] = original.data[1]; copyData[1] = original.data[3];
                copyData[2] = original.data[0]; copyData[3] = original.data[2];
                break;
            case 'X':
                copyData[2] = original.data[7]; copyData[3] = original.data[5];
                copyData[12] = original.data[2]; copyData[14] = original.data[3];
                copyData[21] = original.data[12]; copyData[20] = original.data[14];
                copyData[7] = original.data[21]; copyData[5] = original.data[20];
                copyData[8] = original.data[10]; copyData[9] = original.data[8];
                copyData[10] = original.data[11]; copyData[11] = original.data[9];
                break;
            case 'Y':
                copyData[2] = original.data[12]; copyData[3] = original.data[14];
                copyData[12] = original.data[21]; copyData[14] = original.data[20];
                copyData[21] = original.data[7]; copyData[20] = original.data[5];
                copyData[7] = original.data[2]; copyData[5] = original.data[3];
                copyData[8] = original.data[9]; copyData[9] = original.data[11];
                copyData[10] = original.data[8]; copyData[11] = original.data[10];
                break;
        }
        return new Cube(copyData);
    }

    public static Map<Cube, Map<Cube, Character>> generateCubeSpace(char[] MOVES) {

        Map<Cube, Map<Cube, Character>> graph = new HashMap<>(); // create empty graph
        Cube solvedState = new Cube("WWWWRRRRBBBBOOOOGGGGYYYY"); // generate solved state
        Queue<Cube> queue = new LinkedList<>(); // create a FIFO queue that keeps track of which states to visit next
        queue.add(solvedState); // add solved state to the queue
        graph.put(solvedState, new HashMap<>()); // add solved state to the graph

        while (!queue.isEmpty()) { // while we still have states to visit:
            Cube currentState = queue.remove(); // look at the state (A) we are visiting, and remove them from the queue
            for (char move : MOVES) { // for each possible move:
                Cube newState = applyMove(currentState, move);  // look at the state (B) that is found from applying a
                                                                // move to the state (A) we are currently visiting
                if (!graph.containsKey(newState)) { // if the new state (B) is NOT already in the graph (which means we
                                                    // haven't found it before), then:
                    graph.put(newState, new HashMap<>()); // we add the new state (B) to the graph
                    queue.add(newState); // we add the new state (B) to the queue
                }
                graph.get(currentState).put(newState, move);    // whether or not the new state(B) is already in the
                                                                // graph, we must place an edge from (B) --> (A) with
                                                                // the proper move labeled.
            }
        }
        return graph; // once we stop generating any new states from this exploration, we are done.
    }

    public static void saveToCsv(Map<Cube, Map<Cube, Character>> data, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.append("OuterKey,InnerKey,Value\n");

            // Write data
            for (Map.Entry<Cube, Map<Cube, Character>> outerEntry : data.entrySet()) {
                Cube source = outerEntry.getKey();
                for (Map.Entry<Cube, Character> innerEntry : outerEntry.getValue().entrySet()) {
                    writer.append(source.toString()).append(",");
                    writer.append(innerEntry.getKey().toString()).append(",");
                    writer.append(innerEntry.getValue().toString()).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<Cube, Map<Cube, Character>> loadFromCsv(String filePath) {
        Map<Cube, Map<Cube, Character>> data = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) { // Skip header
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String outerKey = parts[0];
                    String innerKey = parts[1];
                    char value = parts[2].charAt(0);

                    data.computeIfAbsent(new Cube(outerKey), k -> new HashMap<>()).put(new Cube(innerKey), value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}