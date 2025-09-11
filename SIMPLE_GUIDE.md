# 📋 Simple Guide: Site Data Sync System

## What This System Does

This is like a **digital assistant** that automatically collects information about different office locations (called "sites") and organizes their phone number details.

## 🎯 Main Purpose

Think of it like having a robot that:
1. **Looks up** all your office locations from a master list
2. **Finds** phone number details for each location
3. **Saves** all this information in a database
4. **Runs automatically** every 24 hours

## 🔄 How It Works (Simple Version)

### Step 1: Get Office List
- The system asks: "What offices do we have?"
- Gets a list like: "New York Office", "London Office", "Tokyo Office"

### Step 2: Get Phone Details
- For each office, it asks: "What phone numbers are available here?"
- Gets details like:
  - Phone number ranges (1000-1999, 2000-2999)
  - Country codes (+1, +44, +81)
  - Types of numbers (internal, external)

### Step 3: Save Everything
- All this information gets stored in a database
- You can look it up anytime you need it

## 🕐 When Does It Run?

- **Automatically**: Every 24 hours (once per day)
- **Time**: Starts at midnight and runs in the background
- **No manual work needed**: It just runs by itself

## 📊 What Information Gets Collected?

For each office location, the system collects:

| Information | Example |
|-------------|---------|
| **Office Name** | "New York Office" |
| **System ID** | "CM1" |
| **Phone Ranges** | 1000-1999, 2000-2999 |
| **Country Code** | "+1" |
| **Number Type** | Internal, External |

## 🗄️ Where Is Data Stored?

- **Database**: MySQL (like a digital filing cabinet)
- **Database Name**: `amsp`
- **Location**: On your server computer

## 🔧 What You Need to Know

### To Run This System:
1. **Database**: Make sure MySQL is running
2. **Start the Application**: Run the program
3. **Wait**: It will start working automatically

### What Happens If Something Goes Wrong:
- The system will try again automatically
- If it still fails, it will log the error
- You can check the logs to see what happened

## 📱 How to Check If It's Working

### Check System Health:
- Visit: `http://your-server:8080/api/scheduler/health`
- Should show: "status": "UP"

### Check Starfish Data:
- Visit: `http://your-server:8080/api/scheduler/starfish/sites`
- Shows all the office locations found

### Check Phone Details for an Office:
- Visit: `http://your-server:8080/ProvisioningWebService/sps/v1/site?SiteName=OfficeName`
- Shows phone number details for that specific office

## 🎯 Benefits

1. **Automatic**: No manual work needed
2. **Reliable**: Runs every day without fail
3. **Organized**: All data is neatly stored and easy to find
4. **Up-to-date**: Always has the latest information
5. **Efficient**: Saves time and reduces errors

## 🚨 Important Notes

- The system runs **automatically** - you don't need to do anything
- It collects data from **two sources**:
  - Master list of offices
  - Phone number database
- All data is stored in the **`amsp` database**
- The system is designed to be **simple and reliable**

## 📞 Need Help?

If something isn't working:
1. Check if the database is running
2. Check if the application is running
3. Look at the system logs for error messages
4. Make sure all the required services are available

---

**Remember**: This system is designed to work automatically. Just start it up and let it do its job! 🚀
