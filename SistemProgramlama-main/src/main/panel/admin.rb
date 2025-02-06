require_relative 'message_pb'
require_relative 'configuration_pb'
require_relative 'capacity_pb'
require 'socket'

CONFIG_FILE = "dist_server.conf"
SERVER_HOST = "localhost"
SERVER_PORTS = [5000, 5001, 5002]
PLOTTING_SERVER_PORT = 7000

def read_config(config_file)
  fault_tolerance_level = 0 
  begin
    File.open(config_file, 'r') do |file|
      file.each_line do |line|
        fault_tolerance_level = line.split('=', 2).last.to_i
      end
    end
  rescue Errno::ENOENT
    puts "Configuration file not found."
  rescue => e
    puts "Error while reading the configuration file: #{e.message}"
  end
  fault_tolerance_level
end

def send_config(socket, fault_tolerance_level)
  begin
    config = Info::Configuration.new(fault_tolerance_level: fault_tolerance_level, method_type: Info::MethodType::START)
    puts "Sending Fault Tolerance Level: #{fault_tolerance_level}"
    config_proto = config.to_proto
    socket.write([config_proto.size].pack("N") + config_proto)
    puts "Configuration sent."

    response_length = socket.read(4).unpack("N")[0]
    response_proto = socket.read(response_length)
    response = Info::Message.decode(response_proto)
    puts "Response received from server: #{response.demand}, #{response.response}"

    response
  rescue IOError
    puts "Error communicating with the server."
  rescue => e
    puts "Error while sending configuration: #{e.message}"
  end
end

def connect_to_servers
  connections = {}

  SERVER_PORTS.each_with_index do |port, index|
    server_id = index + 1
    begin
      socket = TCPSocket.new(SERVER_HOST, port)
      puts "Connected to Server #{server_id} on port #{port}."
      connections[server_id] = socket
    rescue Errno::ECONNREFUSED
      puts "Failed to connect to Server #{server_id} (port: #{port})."
    rescue => e
      puts "Error while connecting to Server #{server_id}: #{e.message}"
    end
  end

  connections
end

def request_capacity(socket, server_id)
  begin
    request = Info::Message.new(demand: Info::Demand::CPCTY)
    request_proto = request.to_proto
    socket.write([request_proto.size].pack("N") + request_proto)
    puts "Capacity request sent."

    capacity_data = Info::Capacity.new(server_id: server_id, timestamp: Time.now.to_i)
    capacity_proto = capacity_data.to_proto
    socket.write([capacity_proto.size].pack("N") + capacity_proto)
    puts "Capacity request sent successfully."

    response_length = socket.read(4).unpack("N")[0]
    response_proto = socket.read(response_length)
    response = Info::Capacity.decode(response_proto)
    puts "Capacity received: Server#{response.server_id}, Status: #{response.serverX_status}, Timestamp: #{response.timestamp}"

    response
  rescue IOError
    puts "Error communicating with the server."
  rescue => e
    puts "Error while requesting capacity: #{e.message}"
  end
end

def connect_to_plotting_server
  begin
    socket = TCPSocket.new(SERVER_HOST, PLOTTING_SERVER_PORT)
    puts "Connected to Plotting Server on port #{PLOTTING_SERVER_PORT}."
    return socket
  rescue Errno::ECONNREFUSED
    puts "Failed to connect to Plotting Server (port: #{PLOTTING_SERVER_PORT})."
  rescue => e
    puts "Error while connecting to the Plotting Server: #{e.message}"
  end
  nil
end

def send_capacity_to_plotter(socket, capacity, server_id)
  begin
    capacity_to_plot = Info::Capacity.new(
      server_id: server_id,
      serverX_status: capacity.serverX_status,
      timestamp: capacity.timestamp
    )
    capacity_proto = capacity_to_plot.to_proto
    socket.write([capacity_proto.size].pack("N") + capacity_proto)
    puts "Capacity data sent to Plotting Server for Server#{server_id}."
  rescue IOError
    puts "Error communicating with the Plotting Server."
  rescue => e
    puts "Error while sending capacity to Plotting Server: #{e.message}"
  end
end

def handle_server_requests(server_connections, plotting_connection, fault_tolerance_level)
  responses = []

  server_connections.each_value do |connection|
    response = send_config(connection, fault_tolerance_level)
    responses << response if response
  end

  loop do
    server_connections.each_with_index do |(server_id, connection), index|
      begin
        puts "Processing Server #{server_id}."

        if responses[index]&.response.to_s == "YEP"
          capacity = request_capacity(connection, server_id)

          if capacity
            send_capacity_to_plotter(plotting_connection, capacity, server_id)
          else
            puts "Failed to retrieve capacity data from Server #{server_id}."
          end
        else
          puts "Server configuration error."
        end
      rescue IOError
        puts "Connection with Server #{server_id} lost."
      rescue => e
        puts "Error while processing Server #{server_id}: #{e.message}"
      end
    end
    sleep(5)
  end
end

def handle_connections(server_connections, plotting_connection, fault_tolerance_level)
  loop do
    if server_connections.any? && plotting_connection
      handle_server_requests(server_connections, plotting_connection, fault_tolerance_level)
    else
      puts "Failed to establish server connections, retrying..."
      sleep(5)

      loop do
        plotting_connection = connect_to_plotting_server
        server_connections = connect_to_servers

        if server_connections.any? && plotting_connection
          handle_server_requests(server_connections, plotting_connection, fault_tolerance_level)
          break
        end
      end
    end
  end
end

fault_tolerance_level = read_config(CONFIG_FILE)
plotting_connection = connect_to_plotting_server
server_connections = connect_to_servers

handle_connections(server_connections, plotting_connection, fault_tolerance_level)
