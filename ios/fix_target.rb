require 'xcodeproj'

project_path = './CIMDriver.xcodeproj'
project = Xcodeproj::Project.open(project_path)

# Find the widget target
widget_target = project.targets.find { |t| t.name == 'CIMDriverWidgetExtension' }

if widget_target.nil?
  puts "Target CIMDriverWidgetExtension not found"
  exit 1
end

# Find the file reference for DashboardWidget.swift
file_ref = project.files.find { |f| f.path.include?('DashboardWidget.swift') }

if file_ref.nil?
  puts "DashboardWidget.swift not found in project"
  exit 1
end

# Check if it's already in the target
unless widget_target.source_build_phase.files_references.include?(file_ref)
  widget_target.source_build_phase.add_file_reference(file_ref)
  project.save
  puts "Successfully added DashboardWidget.swift to CIMDriverWidgetExtension target!"
else
  puts "DashboardWidget.swift is already in the target."
end
