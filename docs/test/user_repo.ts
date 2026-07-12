import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like } from 'typeorm';
import { User } from './user.entity';
import * as bcrypt from 'bcrypt';

@Injectable()
export class UserRepository {
  constructor(
    @InjectRepository(User)
    private readonly repo: Repository<User>,
  ) {}

  async findByUsername(username: string): Promise<User | null> {
    return this.repo.findOne({ where: { username } });
  }

  async searchByName(keyword: string, limit = 10): Promise<User[]> {
    return this.repo.find({
      where: { displayName: Like(`%${keyword}%`) },
      take: limit,
    });
  }

  async createUser(username: string, rawPassword: string, email: string): Promise<User> {
    const hashed = await bcrypt.hash(rawPassword, 12);
    return this.repo.save({ username, passwordHash: hashed, email });
  }
}
