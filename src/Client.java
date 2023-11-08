import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner;


public class Client {
	DatagramSocket socket;

	public Client() throws SocketException {
		socket = new DatagramSocket();
	}

	static final int RETRY_LIMIT = 4;	/* 
	 * UTILITY METHODS PROVIDED FOR YOU 
	 * Do NOT edit the following functions:
	 *      exitErr
	 *      checksum
	 *      checkFile
	 *      isCorrupted  
	 *      
	 */

	/* exit unimplemented method */
	public void exitErr(String msg) {
		System.out.println("Error: " + msg);
		System.exit(0);
	}	

	/* calculate the segment checksum by adding the payload */
	public int checksum(String content, boolean corrupted)
	{
		if (!corrupted)  
		{
			int i; 
			int sum = 0;
			for (i = 0; i < content.length(); i++)
				sum += (int)content.charAt(i);
			return sum;
		}
		return 0;
	}


	/* check if the input file does exist */
	File checkFile(String fileName)
	{
		File file = new File(fileName);
		if(!file.exists()) {
			System.out.println("SENDER: File does not exists"); 
			System.out.println("SENDER: Exit .."); 
			System.exit(0);
		}
		return file;
	}


	/* 
	 * returns true with the given probability 
	 * 
	 * The result can be passed to the checksum function to "corrupt" a 
	 * checksum with the given probability to simulate network errors in 
	 * file transfer 
	 */
	public boolean isCorrupted(float prob)
	{ 
		double randomValue = Math.random();   
		return randomValue <= prob; 
	}



	/*
	 * The main method for the client.
	 * Do NOT change anything in this method.
	 *
	 * Only specify one transfer mode. That is, either nm or wt
	 */

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 5) {
			System.err.println("Usage: java Client <host name> <port number> <input file name> <output file name> <nm|wt>");
			System.err.println("host name: is server IP address (e.g. 127.0.0.1) ");
			System.err.println("port number: is a positive number in the range 1025 to 65535");
			System.err.println("input file name: is the file to send");
			System.err.println("output file name: is the name of the output file");
			System.err.println("nm selects normal transfer|wt selects transfer with time out");
			System.exit(1);
		}

		Client client = new Client();
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		InetAddress ip = InetAddress.getByName(hostName);
		File file = client.checkFile(args[2]);
		String outputFile =  args[3];
		System.out.println ("----------------------------------------------------");
		System.out.println ("SENDER: File "+ args[2] +" exists  " );
		System.out.println ("----------------------------------------------------");
		System.out.println ("----------------------------------------------------");
		String choice=args[4];
		float loss = 0;
		Scanner sc=new Scanner(System.in);  


		System.out.println ("SENDER: Sending meta data");
		client.sendMetaData(portNumber, ip, file, outputFile); 

		if (choice.equalsIgnoreCase("wt")) {
			System.out.println("Enter the probability of a corrupted checksum (between 0 and 1): ");
			loss = sc.nextFloat();
		} 

		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");
		switch(choice)
		{
		case "nm":
			client.sendFileNormal (portNumber, ip, file);
			break;

		case "wt": 
			client.sendFileWithTimeOut(portNumber, ip, file, loss);
			break; 
		default:
			System.out.println("Error! mode is not recognised");
		} 


		System.out.println("SENDER: File is sent\n");
		sc.close(); 
	}


	/*
	 * THE THREE METHODS THAT YOU HAVE TO IMPLEMENT FOR PART 1 and PART 2
	 * 
	 * Do not change any method signatures 
	 */

	/* TODO: send metadata (file size and file name to create) to the server 
	 * outputFile: is the name of the file that the server will create
	*/
	// Sends meta data information over the network using UPD protocol
	public void sendMetaData(int portNumber, InetAddress IPAddress, File file, String outputFile) throws IOException {
		MetaData metaData = new MetaData();
		metaData.setName(outputFile); // set the name to the name of the file
		metaData.setSize((int) file.length()); // set the size to the length of the file

		// Create a ByteArrayOutputStream to hold the serialized object
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		// Create an ObjectOutputStream to write objects to the ByteArrayOutputStream
		ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
		// Write the metadata object to the ObjectOutputStream
		objectStream.writeObject(metaData);

		// Convert the data in the ByteArrayOutputStream to a byte array
		byte[] data = outputStream.toByteArray();
		// Create a DatagramPacket to be sent over the network
		DatagramPacket sentPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);
		// Send the packet through the socket
		socket.send(sentPacket);

		// Print the metadata information that has been sent
		System.out.println("SENDER: Meta data is sent: (file name, size): ( "+ metaData.getName()+", " + metaData.getSize()+")");
		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");


	}


	/* TODO: Send the file to the server without corruption*/
	public void sendFileNormal(int portNumber, InetAddress IPAddress, File file) throws IOException {
		DatagramSocket socket = new DatagramSocket();

		// Reading the file content as byte array
		byte[] fileContent = Files.readAllBytes(file.toPath());

		// Create segments and send them
		int segmentSize = 4;
		for (int i = 0; i < fileContent.length; i += segmentSize) {
			byte[] segmentData = Arrays.copyOfRange(fileContent, i, Math.min(i + segmentSize, fileContent.length));

			// Create a segment object
			Segment segment = new Segment();
			segment.setSize(segmentData.length);
			segment.setSq(i / segmentSize);
			segment.setType(SegmentType.Data);
			segment.setPayLoad(new String(segmentData));

			//Calculate checksum
			int calculatedChecksum = checksum(new String(segmentData), true);
			segment.setChecksum(calculatedChecksum);

			// Create a ByteArrayOutputStream to hold the serialized object
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			// Create an ObjectOutputStream to write objects to the ByteArrayOutputStream
			ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
			// Write the segment object to the ObjectOutputStream
			objectStream.writeObject(segment);

			// Convert the data in the ByteArrayOutputStream to a byte array
			byte[] data = outputStream.toByteArray();
			// Create a DatagramPacket to be sent over the network
			DatagramPacket sentPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);
			// Send the packet through the socket
			socket.send(sentPacket);

			// Print the segment information that has been sent
			//System.out.println("SENDER: Segment is sent: (segment number, size): (" + segment.getSq() + ", " + segment.getSize() + ")");
			System.out.println("SENDER: Segment sent:(" + segment.getPayLoad() + "), segment number:"+segment.getSq()+", Checksum:"+ segment.getChecksum());
			System.out.println("SENDER: Waiting for ack");

			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			socket.setSoTimeout(1000);

			try{
				socket.receive(receivePacket);
				// Deserialize the received acknowledgment message
				ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
				Segment receivedSegment = (Segment) objectInputStream.readObject();

				// Extract the acknowledgment type and sequence number from the received segment
				SegmentType ackType = receivedSegment.getType();
				int ackSq = receivedSegment.getSq();

				System.out.println(ackType + " " + ackSq);
				// Compare the acknowledgment type and sequence number with the expected values
				if (ackType == SegmentType.Ack && ackSq == segment.getSq()) {
					System.out.println("SENDER: ACK for segment " + segment.getSq() + " received.");
				} else {
					System.out.println("SENDER: Unexpected ACK received.");
				}

			}catch (SocketTimeoutException e){
				System.out.println("SENDER: Timeout occurred. Resending segment " + segment.getSq());
				// Resends segment
				i -= segmentSize;

			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

			//System.out.println("SENDER: ACK sq = " + segment.getSq() + " RECIEVED");
			System.out.println("------------------------------------------------------------------");
		}
	}




	/* TODO: This function is essentially the same as the sendFileNormal function
	 *      except that it resends data segments if no ACK for a segment is
	 *      received from the server.*/
	public void sendFileWithTimeOut(int portNumber, InetAddress IPAddress, File file, float loss) throws IOException {
		DatagramSocket socket = new DatagramSocket();

		// Calls isCorrupted
		Client client = new Client();



		// Reading the file content as byte array
		byte[] fileContent = Files.readAllBytes(file.toPath());

		// Create segments and send them
		int segmentSize = 4;
		for (int i = 0; i < fileContent.length; i += segmentSize) {
			byte[] segmentData = Arrays.copyOfRange(fileContent, i, Math.min(i + segmentSize, fileContent.length));

			// Create a segment object
			Segment segment = new Segment();
			segment.setSize(segmentData.length);
			segment.setSq(i / segmentSize);
			segment.setType(SegmentType.Data);
			segment.setPayLoad(new String(segmentData));

			boolean resultIsCorrupted = client.isCorrupted(loss);
			//Calculate checksum
			int calculatedChecksum = checksum(new String(segmentData), resultIsCorrupted);
			segment.setChecksum(calculatedChecksum);


			// Create a ByteArrayOutputStream to hold the serialized object
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			// Create an ObjectOutputStream to write objects to the ByteArrayOutputStream
			ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
			// Write the segment object to the ObjectOutputStream
			objectStream.writeObject(segment);

			// Convert the data in the ByteArrayOutputStream to a byte array
			byte[] data = outputStream.toByteArray();
			// Create a DatagramPacket to be sent over the network
			DatagramPacket sentPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);
			// Send the packet through the socket
			socket.send(sentPacket);

			// Print the segment information that has been sent
			//System.out.println("SENDER: Segment is sent: (segment number, size): (" + segment.getSq() + ", " + segment.getSize() + ")");
			System.out.println("SENDER: Segment sent:(" + segment.getPayLoad() + "), segment number:"+segment.getSq()+", Checksum:"+ segment.getChecksum());

			if (resultIsCorrupted){
				String ANSI_RED = "\u001B[31m";
				String ANSI_RESET = "\u001B[0m";
				System.out.println(ANSI_RED + "\t\t>>>>>>>Network ERROR: segment checksum is corrupted!<<<<<<<<<");
				System.out.println("SENDER: Sending segment: sq:" + segment.getSq() + " size:" + segment.getSize() + " checksum:" + segment.getChecksum() + "content(" + segment.getPayLoad() + ")" + ANSI_RESET);
			}
			System.out.println("SENDER: Waiting for ack");

			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			socket.setSoTimeout(1000);

			try{
				socket.receive(receivePacket);
				// Deserialize the received acknowledgment message
				ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
				Segment receivedSegment = (Segment) objectInputStream.readObject();

				// Extract the acknowledgment type and sequence number from the received segment
				SegmentType ackType = receivedSegment.getType();
				int ackSq = receivedSegment.getSq();

				System.out.println(ackType + " " + ackSq);
				// Compare the acknowledgment type and sequence number with the expected values
				if (ackType == SegmentType.Ack && ackSq == segment.getSq()) {
					System.out.println("SENDER: ACK for segment " + segment.getSq() + " received.");
				} else {
					System.out.println("SENDER: Unexpected ACK received.");
				}

			}catch (SocketTimeoutException e){
				System.out.println("SENDER: Timeout occurred. Resending segment " + segment.getSq());
				// Resends segment
				i -= segmentSize;

			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

			//System.out.println("SENDER: ACK sq = " + segment.getSq() + " RECIEVED");
			System.out.println("------------------------------------------------------------------");
		}
	}


}


