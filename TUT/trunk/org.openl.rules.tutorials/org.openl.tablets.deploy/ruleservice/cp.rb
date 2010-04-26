require 'fileutils'
require 'find'

def go(dir)
  Find.find(dir) do |f|
    yield f if !FileTest.directory?(f) && f[-4..-1] == ".jar"
  end
end


a = []
go("lib") do |f|
  a << f
end

puts a.join(";")
