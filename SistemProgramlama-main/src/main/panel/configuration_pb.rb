# frozen_string_literal: true
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: configuration.proto

require 'google/protobuf'


descriptor_data = "\n\x13\x63onfiguration.proto\x12\x04info\"U\n\rConfiguration\x12\x1d\n\x15\x66\x61ult_tolerance_level\x18\x01 \x01(\x05\x12%\n\x0bmethod_type\x18\x02 \x01(\x0e\x32\x10.info.MethodType*!\n\nMethodType\x12\t\n\x05START\x10\x00\x12\x08\n\x04STOP\x10\x01\x42&\n\x10\x63omm.struct.infoB\x12\x43onfigurationProtob\x06proto3"

pool = Google::Protobuf::DescriptorPool.generated_pool
pool.add_serialized_file(descriptor_data) 

module Info
  Configuration = ::Google::Protobuf::DescriptorPool.generated_pool.lookup("info.Configuration").msgclass
  MethodType = ::Google::Protobuf::DescriptorPool.generated_pool.lookup("info.MethodType").enummodule
end
